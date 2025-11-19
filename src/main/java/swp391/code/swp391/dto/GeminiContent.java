package swp391.code.swp391.dto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
// { "parts": [ ... ] }
@Getter
public class GeminiContent {
    @Setter
    private List<GeminiPart> parts;
    private final String role;

    public GeminiContent(List<GeminiPart> parts, String role) {
        this.parts = parts;
        this.role = role;
    }

    public static GeminiContent from(String text) {
        return new GeminiContent(List.of(new GeminiPart(text)), "user");
    }

}