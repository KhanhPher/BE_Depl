package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.GeminiChatDecision;
import swp391.code.swp391.dto.ProactiveSuggestionDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE TẠO GỢI Ý CHỦĐỘNG
 * Dựa vào context và intent để tạo ra các suggestion hữu ích cho user
 */
@Service
@RequiredArgsConstructor
public class ProactiveSuggestionService {

    /**
     * Tạo gợi ý dựa trên decision từ Gemini
     */
    public List<ProactiveSuggestionDTO> generateSuggestions(GeminiChatDecision decision, String lastUserMessage) {
        List<ProactiveSuggestionDTO> suggestions = new ArrayList<>();

        String intent = decision.getIntent();

        // Case 1: User hỏi về trạm sạc
        if (intent != null && intent.equals("ASKING_QUESTION")) {
            if (containsKeywords(lastUserMessage, "trạm", "station", "chỗ")) {
                suggestions.add(ProactiveSuggestionDTO.quickAction(
                    "Tìm trạm gần nhất",
                    "Xem trạm",
                    "FIND_NEAREST_STATION"
                ));

                suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                    "Giờ cao điểm ở trạm này là khi nào?"
                ));
            }

            // Nếu user hỏi về giá
            if (containsKeywords(lastUserMessage, "giá", "phí", "cost", "price")) {
                suggestions.add(ProactiveSuggestionDTO.quickAction(
                    "Xem bảng giá đầy đủ",
                    "Xem giá",
                    "VIEW_PRICING"
                ));
            }

            // Nếu user hỏi về xe
            if (containsKeywords(lastUserMessage, "xe", "car", "vehicle")) {
                suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                    "Loại connector nào phù hợp với xe của tôi?"
                ));
            }
        }

        // Case 2: User báo lỗi
        else if (intent != null && intent.equals("REPORTING_ISSUE")) {
            suggestions.add(ProactiveSuggestionDTO.helpfulTip(
                "💡 Mẹo",
                "Bạn có thể theo dõi trạng thái xử lý báo cáo tại mục 'Lịch sử báo cáo'"
            ));

            suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                "Có trạm nào khác gần đây không?"
            ));
        }

        // Case 3: User hỏi về availability
        else if (intent != null && intent.equals("CHECK_AVAILABILITY")) {
            suggestions.add(ProactiveSuggestionDTO.quickAction(
                "Đặt chỗ ngay",
                "Book Now",
                "BOOK_NOW"
            ));

            suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                "Có thể đặt trước không?"
            ));
        }

        // Case 4: Sentiment tiêu cực - cung cấp support
        if ("NEGATIVE".equals(decision.getSentiment())) {
            suggestions.add(ProactiveSuggestionDTO.helpfulTip(
                "🆘 Cần hỗ trợ?",
                "Liên hệ hotline: 1900-xxxx hoặc chat với nhân viên"
            ));
        }

        // Case 5: Default - always offer these
        if (suggestions.isEmpty()) {
            suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                "Làm thế nào để đặt chỗ sạc?"
            ));

            suggestions.add(ProactiveSuggestionDTO.relatedQuestion(
                "Trạm nào gần tôi nhất?"
            ));
        }

        // Limit to 3 suggestions
        return suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
    }

    /**
     * Helper: Kiểm tra message có chứa keywords không
     */
    private boolean containsKeywords(String message, String... keywords) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        for (String keyword : keywords) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

