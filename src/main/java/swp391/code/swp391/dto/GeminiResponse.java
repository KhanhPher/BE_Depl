package swp391.code.swp391.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import swp391.code.swp391.dto.GeminiContent;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private List<Candidate> candidates;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private GeminiContent content;
        public GeminiContent getContent() { return content; }
        public void setContent(GeminiContent content) { this.content = content; }
    }
    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    public String getFirstTextResponse() {
        try {
            return this.candidates.get(0).getContent().getParts().get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }
}