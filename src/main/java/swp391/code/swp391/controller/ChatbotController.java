package swp391.code.swp391.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.service.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired private GeminiService geminiService;
    @Autowired private IssueReportService issueReportService;
    // TIÊM "BỘ NHỚ" SESSION VÀO CONTROLLER
    @Autowired
    private ChatHistory chatHistory;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private ChargingStationService chargingStationService;
    @Autowired
    private ProactiveSuggestionService proactiveSuggestionService;

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> handleChatMessage(@RequestBody ChatRequest chatRequest) {

        // 1. Thêm tin nhắn mới của user vào "bộ nhớ"
        chatHistory.addMessage("user", chatRequest.getMessage());

        // 2. Lấy TOÀN BỘ lịch sử ra
        List<ChatMessage> fullHistory = chatHistory.getHistory();

        // 3. Gửi TOÀN BỘ lịch sử cho Gemini
        GeminiChatDecision decision = geminiService.handleChatIntent(fullHistory);

        // 4. Lấy câu trả lời của Bot
        String botReply = decision.getChatResponse();

        // Xử lý lỗi (Rate Limit, Lỗi Parse)
        if ("ERROR".equals(decision.getIntent())) {
            return ResponseEntity.ok(new ChatResponse(botReply));
        }
        // Xử lý báo lỗi
        else if ("REPORTING_ISSUE".equals(decision.getIntent())) {
            if (decision.getReportDetails() != null && decision.getReportDetails().getStationName() != null) {
                try {
                    // Gemini đã nhớ ngữ cảnh và bóc tách được
                    issueReportService.createReportFromParsedData(
                            decision.getReportDetails(),
                            chatRequest.getMessage() // Hoặc gửi toàn bộ history
                    );

                    // Tạo response rõ ràng cho user
                    String issueType = decision.getReportDetails().getIssueType() != null ?
                            decision.getReportDetails().getIssueType() : "sự cố";
                    botReply = String.format("Cảm ơn bạn đã báo cáo! Bạn đã báo cáo cho quản trị viên vấn đề: %s tại trạm %s. " +
                            "Chúng tôi sẽ xử lý sớm nhất có thể. Bạn sẽ nhận được thông báo về tình trạng xử lý.",
                            issueType,
                            decision.getReportDetails().getStationName());
                } catch (Exception e) {
                    return ResponseEntity.ok(new ChatResponse("Tôi ghi nhận lỗi, nhưng: " + e.getMessage()));
                }
            } else {
                return ResponseEntity.ok(new ChatResponse("Tôi hiểu bạn báo lỗi, nhưng vui lòng cung cấp TÊN TRẠM nhé."));
            }
        }
        else if ("FIND_NEARBY_STATION".equals(decision.getIntent())) {
            botReply = handleFindNearbyStation(decision);
        }
        else if ("CHECK_AVAILABILITY".equals(decision.getIntent())) {
            botReply = handleAvailabilityCheck(decision);
        }
        // Xử lý hỏi đáp (Mặc định)
        else {
            botReply = decision.getChatResponse();
        }
        // Lưu câu trả lời của Bot vào "bộ nhớ"
        chatHistory.addMessage("model", botReply);

        // NEW: Tạo proactive suggestions
        List<ProactiveSuggestionDTO> suggestions =
            proactiveSuggestionService.generateSuggestions(decision, chatRequest.getMessage());

        // Trả về câu trả lời của Bot với các thông tin mới
        return ResponseEntity.ok(ChatResponse.builder()
                .reply(botReply)
                .userSentiment(decision.getSentiment())
                .confidence(decision.getConfidence())
                .suggestions(suggestions)
                .detectedLanguage("vi")
                .build());
    }

    /**
     * Hàm helper xử lý tìm trạm gần vị trí
     */
    private String handleFindNearbyStation(GeminiChatDecision decision) {
        Double latitude = decision.getLatitude();
        Double longitude = decision.getLongitude();

        if (latitude == null || longitude == null) {
            return "Tôi cần tọa độ chính xác để tìm trạm gần bạn. Vui lòng cho tôi biết latitude và longitude của bạn.";
        }

        try {
            // Tìm top 5 trạm gần nhất
            List<ChargingStationDTO> nearbyStations = chargingStationService.findNearbyStations(
                latitude,
                longitude,
                5.0  // Bán kính 5km
            );

            if (nearbyStations == null || nearbyStations.isEmpty()) {
                return String.format("Rất tiếc, tôi không tìm thấy trạm sạc nào trong bán kính 5km từ vị trí của bạn (%.6f, %.6f). " +
                    "Bạn có thể thử tìm kiếm trong khu vực khác không?", latitude, longitude);
            }

            // Tạo response đẹp với danh sách trạm
            StringBuilder response = new StringBuilder();
            response.append(String.format("Tôi tìm thấy %d trạm sạc gần vị trí của bạn:\n\n", nearbyStations.size()));

            for (int i = 0; i < nearbyStations.size(); i++) {
                ChargingStationDTO station = nearbyStations.get(i);
                double distance = station.getDistance() != null ? station.getDistance() : 0.0;

                response.append(String.format("%d. **%s**\n", i + 1, station.getStationName()));
                response.append(String.format("    %s\n", station.getAddress()));
                response.append(String.format("    Cách bạn: %.2f km\n", distance));

                if (station.getChargingPointNumber() > 0) {
                    response.append(String.format("    Số trụ sạc: %d\n", station.getChargingPointNumber()));
                }

                response.append("\n");
            }

            response.append("Bạn muốn biết thêm thông tin về trạm nào?");

            return response.toString();

        } catch (Exception e) {
            return String.format("Rất tiếc, tôi gặp lỗi khi tìm trạm gần vị trí (%.6f, %.6f). Vui lòng thử lại sau.",
                latitude, longitude);
        }
    }

    /**
     * Hàm helper mới để xử lý logic dự đoán
     */
    private String handleAvailabilityCheck(GeminiChatDecision decision) {
        String userInputStationName = decision.getStationName();
        Integer hour = decision.getHour();

        if (userInputStationName == null || hour == null) {
            return "Tôi chưa rõ bạn muốn hỏi trạm nào và vào lúc mấy giờ. Bạn có thể nói rõ hơn không?";
        }

        // Sử dụng Gemini để tìm tên trạm chính xác từ input của người dùng
        String exactStationName = geminiService.findClosestStationName(userInputStationName);

        if (exactStationName == null) {
            return "Rất tiếc, tôi không tìm thấy trạm sạc nào có tên giống \"" + userInputStationName +
                   "\". Bạn có thể kiểm tra lại tên trạm không?";
        }

        // Tìm stationId từ tên chính xác
        Long stationId = chargingStationService.findStationIdByName(exactStationName);
        if (stationId == null) {
            return "Rất tiếc, tôi không tìm thấy ID của trạm sạc \"" + exactStationName + "\"";
        }

        try {
            // 1. Lấy dữ liệu giờ cao điểm trung bình 30 ngày qua
            List<PeakHourDTO> peakData = analyticsService.getPeakHours(
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    stationId
            );

            // 2. Tìm mức độ cao điểm của giờ đó
            String peakLevel = "UNKNOWN";
            for (PeakHourDTO dto : peakData) {
                if (dto.getHour().equals(hour)) {
                    peakLevel = dto.getPeakLevel(); // "HIGH", "LOW", "MEDIUM"
                    break;
                }
            }

            // 3. Trả lời thông minh
            switch (peakLevel) {
                case "VERY_HIGH":
                case "HIGH":
                    return String.format("Dựa trên dữ liệu lịch sử, %d:00 là giờ CAO ĐIỂM tại %s. Khả năng cao trạm sẽ rất đông, bạn nên cân nhắc đi giờ khác.", hour, exactStationName);
                case "MEDIUM":
                    return String.format("Dựa trên dữ liệu lịch sử, %d:00 là giờ trung bình tại %s. Có thể sẽ có vài người đang sạc.", hour, exactStationName);
                case "LOW":
                    return String.format("Dựa trên dữ liệu lịch sử, %d:00 là giờ thấp điểm tại %s. Khả năng cao trạm sẽ vắng và có nhiều chỗ trống.", hour, exactStationName);
                default:
                    return String.format("Tôi đã tìm thấy trạm \"%s\", nhưng chưa có đủ dữ liệu lịch sử về giờ cao điểm tại trạm này.", exactStationName);
            }

        } catch (Exception e) {
            return "Rất tiếc, tôi chưa thể tra cứu dữ liệu giờ cao điểm cho trạm " + exactStationName;
        }
    }
}