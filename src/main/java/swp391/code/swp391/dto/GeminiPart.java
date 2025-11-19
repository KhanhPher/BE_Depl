package swp391.code.swp391.dto;

import lombok.Getter;
import lombok.Setter;

// { "text": "..." }
@Setter
@Getter
public class GeminiPart {
    private String text;
    public GeminiPart(String text) { this.text = text; }

}