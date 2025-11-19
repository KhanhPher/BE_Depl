package swp391.code.swp391.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
// Import các DTO mới cho History
import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.ConnectorType;
import swp391.code.swp391.entity.KnowledgeBaseItem;
import swp391.code.swp391.repository.ChargingStationRepository;
import swp391.code.swp391.repository.ConnectorTypeRepository;
import swp391.code.swp391.repository.KnowledgeBaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final GeminiApiCaller apiCaller;
    private final ObjectMapper objectMapper;
    private final ChargingStationRepository stationRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    // NEW: Context Window Manager để tránh vượt token limit
    private final ContextWindowManager contextWindowManager;

    @Autowired
    public GeminiService(GeminiApiCaller apiCaller, ObjectMapper objectMapper,
                         ChargingStationRepository stationRepository,
                         ConnectorTypeRepository connectorTypeRepository,
                         KnowledgeBaseRepository knowledgeBaseRepository,
                         ContextWindowManager contextWindowManager) {
        this.apiCaller = apiCaller;
        this.objectMapper = objectMapper;
        this.stationRepository = stationRepository;
        this.connectorTypeRepository = connectorTypeRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.contextWindowManager = contextWindowManager;
    }

    // --- CÁC HÀM STATELESS (KHÔNG CẦN CONTEXT) ---

    /**
     * HÀM 3.0: Tìm tên trạm gần giống nhất
     * Gemini sẽ so sánh input của user với danh sách trạm và trả về tên chính xác nhất
     */
    public String findClosestStationName(String userInput) {
        String knownStations = getDynamicStationsJson();

        String promptText = String.format(
                """
                Bạn là trợ lý tìm kiếm thông minh.
                Người dùng nhập: "%s"
                Danh sách tên trạm sạc có sẵn: %s
                
                Nhiệm vụ: Tìm tên trạm CHÍNH XÁC nhất khớp với input của người dùng.
                - Nếu tìm thấy tên gần giống (cho phép lỗi chính tả, thiếu dấu, viết tắt), trả về TÊN CHÍNH XÁC từ danh sách.
                - Nếu không tìm thấy, trả về: "NOT_FOUND"
                
                Chỉ trả về TÊN TRẠM hoặc "NOT_FOUND", không giải thích gì thêm.
                """,
                userInput, knownStations
        );

        GeminiRequest request = GeminiRequest.from(promptText);
        String result = callGeminiApi(request);

        if (result == null || result.trim().isEmpty() || result.contains("NOT_FOUND")) {
            return null;
        }

        return result.trim();
    }

    /**
     * HÀM 3.1: Gợi ý Tiện ích Xung quanh
     */
    public String getAmenitiesAround(double latitude, double longitude) {
        String promptText = String.format(
                """
                Bạn là một trợ lý du lịch am hiểu. Tại một trạm sạc xe điện ở vị trí:
                Vĩ độ: %f
                Kinh độ: %f
                Trả lời dưới dạng danh sách gạch đầu dòng, không cần xin chào.
                Hãy gợi ý 3-5 địa điểm tiện ích (quán cafe, cửa hàng tiện lợi, quán ăn...)
                trong bán kính đi bộ 5 phút, cho địa chỉ cụ thể và mô tả ngắn gọn.
                """,
                latitude, longitude
        );
        // Gói prompt vào Request và gọi hàm API private
        GeminiRequest request = GeminiRequest.from(promptText);
        return callGeminiApi(request);
    }

    /**
     * HÀM 3.2: Phân tích Xu hướng Thị trường (Đã sửa + CACHE)
     */
    @org.springframework.cache.annotation.Cacheable(value = "market-trends", key = "'weekly'")
    public String getMarketTrends() {
        logger.info("Fetching fresh market trends from Gemini (cache miss)");
        String promptText = """
            Bạn là một nhà phân tích thị trường xe điện (EV) chuyên nghiệp tại Việt Nam.
            Hãy tóm tắt 3 xu hướng quan trọng nhất trong hôm nay
            liên quan đến thị trường xe điện và trạm sạc tại Việt Nam.
            Tập trung vào:
            1. Mẫu xe điện mới hoặc phổ biến?
            2. Động thái của đối thủ cạnh tranh (VinFast, EBOOST)?
            3. Công nghệ sạc mới?
            Trả lời ngắn gọn dưới dạng gạch đầu dòng. 
            Không cần giới thiệu bản thân, Câu mở đầu là: Xu hướng thị trường trong hôm nay + ngày tháng năm
            """;
        // Gói prompt vào Request và gọi hàm API private
        GeminiRequest request = GeminiRequest.from(promptText);
        return callGeminiApi(request);
    }

    /**
     * HÀM 3.3 (Báo lỗi): Dùng cho SmartReportController
     */
    public ParsedIssueReportDTO parseUserFeedback(String userFeedbackText) {
        String knownStations = getDynamicStationsJson();
        String knownConnectors = getDynamicConnectorsJson();
        String knownIssues = "[\"Không vào điện\", \"Hỏng hóc vật lý\", \"Không kết nối được\", \"Ứng dụng lỗi\", \"Khác\"]";

        String promptText = String.format(
                """
                Phân tích phản hồi của người dùng.
                Chỉ trả lời bằng một cấu trúc JSON duy nhất.
                Dựa vào các danh sách sau:
                Trạm sạc đã biết: %s
                Loại trụ đã biết: %s
                Loại lỗi đã biết: %s
                Cảm xúc: ["TÍCH CỰC", "TIÊU CỰC", "TRUNG LẬP"]
                Phản hồi: "%s"
                
                JSON:
                {
                  "station_name": "[tên trạm]",
                  "connector_type": "[loại trụ]",
                  "issue_type": "[loại lỗi]",
                  "user_sentiment": "[cảm xúc]"
                }
                """,
                knownStations, knownConnectors, knownIssues, userFeedbackText
        );

        // Gói prompt vào Request và gọi hàm API private
        GeminiRequest request = GeminiRequest.from(promptText);
        String jsonString = callGeminiApi(request);

        if (jsonString == null) return null;
        try {
            String cleanJson = jsonString.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(cleanJson, ParsedIssueReportDTO.class);
        } catch (Exception e) {
            logger.error("Lỗi parse JSON báo lỗi: {}", e.getMessage(), e);
            return null;
        }
    }

    // --- HÀM STATEFUL (CẦN CONTEXT) ---

    /**
     * HÀM 3.3 (Chatbot): ĐÃ NÂNG CẤP ĐỂ NHỚ CONTEXT (Đã sửa + OPTIMIZED)
     */
    public GeminiChatDecision handleChatIntent(List<ChatMessage> history) {

        // NEW: Optimize history để tránh vượt token limit
        List<ChatMessage> optimizedHistory = contextWindowManager.optimizeHistory(history);
        int estimatedTokens = contextWindowManager.estimateTokenCount(optimizedHistory);
        logger.info("Processing chat with {} messages (~{} tokens)",
                    optimizedHistory.size(), estimatedTokens);

        // 1. Xây dựng Master Prompt
        String knownStations = getDynamicStationsJson();
        String knownConnectors = getDynamicConnectorsJson();
        String knownIssues = "[\"Không vào điện\", \"Hỏng hóc vật lý\", \"Không kết nối được\"]";
        String dynamicKnowledgeBase = getDynamicKnowledgeBase();

        String masterPrompt = String.format(
                """
                Bạn là trợ lý AI thông minh của hệ thống trạm sạc xe điện ChargeHub, được train bởi Nguyên.
                
                === QUAN TRỌNG: PHÂN TÍCH CONTEXT ===
                - XEM TOÀN BỘ LỊCH SỬ TRÒ CHUYỆN để hiểu ngữ cảnh
                - Nếu user đề cập đến "trạm", "chỗ sạc", "trụ sạc" → họ đang hỏi về TRẠM SẠC
                - Nếu user cho tọa độ/địa điểm → họ muốn TÌM TRẠM GẦN ĐÓ
                - Nếu user hỏi "còn chỗ không", "có trống không" → họ muốn CHECK_AVAILABILITY
                - Nếu user chỉ nói tên mơ hồ (VD: "nhà văn hóa", "buổi chiều") → BẠN PHẢI HỎI LẠI RÕ HƠN
                
                === CÁC INTENT (Ý định) ===
                
                1. "ASKING_ABOUT_STATION" - User hỏi về TRẠM SẠC (tên, vị trí, giá, loại trụ...)
                   Từ khóa: "trạm", "chỗ sạc", "trụ sạc", "ở đâu", "tên trạm", "có trạm nào"
                   Hành động:
                   - Nếu user đã nói TÊN TRẠM rõ ràng → Trả lời thông tin trạm đó
                   - Nếu user nói mơ hồ (VD: "nhà văn hóa") → HỎI LẠI: "Bạn muốn hỏi về trạm sạc nào? Hiện có các trạm: [danh sách]"
                   - Nếu user cho VỊ TRÍ/TỌA ĐỘ → Chuyển sang intent "FIND_NEARBY_STATION"
                   JSON: { "intent": "ASKING_ABOUT_STATION", "answer": "[Câu trả lời hoặc câu hỏi làm rõ]", "station_name": "[tên trạm nếu xác định được]", "sentiment": "POSITIVE/NEGATIVE/NEUTRAL", "confidence": 0.0-1.0 }
                
                2. "FIND_NEARBY_STATION" - User muốn tìm trạm GẦN VỊ TRÍ NÀO ĐÓ
                   Từ khóa: "gần đây", "gần tôi", "gần [địa điểm]", "latitude", "longitude", "tọa độ"
                   Bóc tách: latitude, longitude (nếu có) HOẶC địa điểm (VD: "Quận 1", "Thủ Đức")
                   JSON: { "intent": "FIND_NEARBY_STATION", "answer": "[Câu trả lời chờ]", "location": "[địa điểm]", "latitude": [số hoặc null], "longitude": [số hoặc null], "sentiment": "POSITIVE/NEGATIVE/NEUTRAL" }
                
                3. "CHECK_AVAILABILITY" - User hỏi SLOT TRỐNG, TÌNH TRẠNG trạm
                   Từ khóa: "còn chỗ không", "có trống không", "bận không", "đông không", "lúc mấy giờ", "buổi [sáng/chiều/tối]"
                   Bóc tách: TÊN TRẠM (bắt buộc) + THỜI GIAN (nếu có)
                   - Nếu THIẾU TÊN TRẠM → HỎI LẠI: "Bạn muốn kiểm tra trạm nào?"
                   - Thời gian: "sáng"=7h, "trưa"=12h, "chiều"=15h, "tối"=19h, "đêm"=22h
                   JSON: { "intent": "CHECK_AVAILABILITY", "answer": "[Câu hỏi làm rõ nếu thiếu info]", "station_name": "[tên trạm]", "hour": [0-23 hoặc null], "sentiment": "POSITIVE/NEGATIVE/NEUTRAL" }
                
                4. "ASKING_QUESTION" - Câu hỏi CHUNG về hệ thống (không liên quan trạm cụ thể)
                   VD: "Sạc xe điện là gì?", "Cách đặt chỗ?", "Giá sạc bao nhiêu?"
                   JSON: { "intent": "ASKING_QUESTION", "answer": "[Câu trả lời]", "sentiment": "POSITIVE/NEGATIVE/NEUTRAL", "confidence": 0.0-1.0 }
                
                5. "REPORTING_ISSUE" - User BÁO LỖI
                   Từ khóa: "hỏng", "lỗi", "không hoạt động", "báo cáo", "phản ánh"
                   Bóc tách: tên trạm, loại trụ, loại lỗi (từ LỊCH SỬ)
                   JSON: { "intent": "REPORTING_ISSUE", "answer": "[Xác nhận]", "sentiment": "NEGATIVE", "report_details": { "station_name": "...", "connector_type": "...", "issue_type": "..." } }
                
                === DỮ LIỆU HỆ THỐNG ===
                Trạm sạc có sẵn: %s
                Loại trụ sạc: %s
                Loại lỗi: %s
                
                === KIẾN THỨC CƠ BẢN ===
                %s
                
                === QUY TẮC TRẢ LỜI ===
                1. Nếu user nói MƠ HỒ → HỎI LẠI để làm rõ (tên trạm, thời gian...)
                2. Nếu có TỪ KHÓA TRẠM SẠC → Ưu tiên intent liên quan đến trạm
                3. Nếu có TỌA ĐỘ/ĐỊA ĐIỂM → Intent là "FIND_NEARBY_STATION"
                4. LUÔN TRẢ LỜI LỊCH SỰ, NGẮN GỌN, DỄ HIỂU
                5. Chỉ trả về MỘT JSON DUY NHẤT
                
                ---
                PHÂN TÍCH TIN NHẮN CUỐI VÀ TRẢ VỀ JSON:
                """,
                knownStations, knownConnectors, knownIssues, dynamicKnowledgeBase
        );

        // 2. Xây dựng List<GeminiContent> (Lịch sử chat) - SỬ DỤNG OPTIMIZED HISTORY
        List<GeminiContent> contents = new ArrayList<>();
        for (int i = 0; i < optimizedHistory.size(); i++) {
            ChatMessage msg = optimizedHistory.get(i);
            String text = msg.getText();

            // Gắn master prompt vào tin nhắn đầu tiên của user
            if (i == 0 && "user".equals(msg.getRole())) {
                text = masterPrompt + "\n\nLỊCH SỬ BẮT ĐẦU:\nUser: " + text;
            }

            // Tạo GeminiContent với cấu trúc đúng: (List<GeminiPart>, role)
            List<GeminiPart> parts = List.of(new GeminiPart(text));
            contents.add(new GeminiContent(parts, msg.getRole()));
        }

        // 3. Tạo Request mới
        GeminiRequest geminiRequest = new GeminiRequest(contents, null);

        // 4. Gọi API (Hàm private thống nhất)
        String jsonString = callGeminiApi(geminiRequest);

        if (jsonString == null) {
            return createErrorDecision("Rất tiếc, tôi đang gặp lỗi kết nối (Rate Limit). Vui lòng thử lại sau 1 phút.");
        }

        try {
            String cleanJson = jsonString.replace("```json", "").replace("```", "").trim();
            GeminiChatDecision decision = objectMapper.readValue(cleanJson, GeminiChatDecision.class);

            if (decision.getIntent() == null || decision.getChatResponse() == null) {
                return createErrorDecision("Tôi chưa hiểu rõ ý của bạn. Bạn có thể nói rõ hơn không?");
            }

            // NEW: Ensure sentiment and confidence are set
            if (decision.getSentiment() == null) {
                decision.setSentiment("NEUTRAL");
            }
            if (decision.getConfidence() == null) {
                decision.setConfidence(0.8);
            }

            return decision;

        } catch (Exception e) {
            logger.error("Lỗi parse JSON quyết định: {}", e.getMessage());
            return createErrorDecision(jsonString);
        }
    }

    // --- CÁC HÀM HELPER ---

    /**
     * Hàm gọi API private thống nhất, chấp nhận GeminiRequest (Object)
     */
    @RateLimiter(name = "gemini-limiter", fallbackMethod = "geminiFallback")
    private String callGeminiApi(GeminiRequest requestBody) {
        return apiCaller.callGemini(requestBody);
    }

    /**
     * Fallback cho hàm đã nâng cấp
     */
    private String geminiFallback(GeminiRequest requestBody, RequestNotPermitted ex) {
        logger.warn("RATE LIMIT! Request bị từ chối.");
        return null;
    }

    // (Hàm createErrorDecision)
    private GeminiChatDecision createErrorDecision(String errorMessage) {
        GeminiChatDecision errorDecision = new GeminiChatDecision();
        errorDecision.setIntent("ERROR");
        errorDecision.setChatResponse(errorMessage);
        return errorDecision;
    }

    // (Các hàm getDynamic)
    private String getDynamicKnowledgeBase() {
        try {
            List<KnowledgeBaseItem> items = knowledgeBaseRepository.findAll();
            return items.stream()
                    .map(item -> String.format("- Hỏi: %s? Trả lời: %s.", item.getQuestion(), item.getAnswer()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) { return "Không có kiến thức."; }
    }

    private String getDynamicStationsJson() {
        try {
            List<String> stationNames = stationRepository.findAll().stream().map(ChargingStation::getStationName).toList();
            return objectMapper.writeValueAsString(stationNames);
        } catch (Exception e) { return "[]"; }
    }

    private String getDynamicConnectorsJson() {
        try {
            List<String> connectorNames = connectorTypeRepository.findAll().stream().map(ConnectorType::getTypeName).toList();
            return objectMapper.writeValueAsString(connectorNames);
        } catch (Exception e) { return "[]"; }
    }
}