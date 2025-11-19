package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Proactive Suggestions (Gợi ý chủ động)
 * AI sẽ phân tích context và đưa ra gợi ý hữu ích
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProactiveSuggestionDTO {

    private String type; // "QUICK_ACTION", "RELATED_QUESTION", "HELPFUL_TIP"

    private String title;

    private String description;

    private String actionText; // Text cho button (VD: "Xem trạm gần nhất")

    private String actionPayload; // Data để frontend xử lý

    /**
     * Factory methods for common suggestion types
     */
    public static ProactiveSuggestionDTO quickAction(String title, String actionText, String payload) {
        return ProactiveSuggestionDTO.builder()
            .type("QUICK_ACTION")
            .title(title)
            .actionText(actionText)
            .actionPayload(payload)
            .build();
    }

    public static ProactiveSuggestionDTO relatedQuestion(String question) {
        return ProactiveSuggestionDTO.builder()
            .type("RELATED_QUESTION")
            .title(question)
            .actionText("Hỏi câu này")
            .actionPayload(question)
            .build();
    }

    public static ProactiveSuggestionDTO helpfulTip(String title, String description) {
        return ProactiveSuggestionDTO.builder()
            .type("HELPFUL_TIP")
            .title(title)
            .description(description)
            .build();
    }
}

