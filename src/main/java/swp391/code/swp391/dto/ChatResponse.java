package swp391.code.swp391.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String reply;

    // NEW: Sentiment của user (POSITIVE, NEGATIVE, NEUTRAL)
    private String userSentiment;

    // NEW: Confidence score (0.0 - 1.0)
    private Double confidence;

    // NEW: Proactive suggestions (gợi ý chủ động)
    private List<ProactiveSuggestionDTO> suggestions;

    // NEW: Detected language (vi, en)
    private String detectedLanguage;

    // Backward compatible constructor
    public ChatResponse(String reply) {
        this.reply = reply;
        this.confidence = 1.0;
        this.detectedLanguage = "vi";
    }
}

