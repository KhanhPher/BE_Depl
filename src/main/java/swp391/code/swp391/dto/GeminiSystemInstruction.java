package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import swp391.code.swp391.dto.GeminiPart;

import java.util.List;

@Data
@AllArgsConstructor
public class GeminiSystemInstruction {
    private List<GeminiPart> parts;

    public static GeminiSystemInstruction from(String text) {
        return new GeminiSystemInstruction(List.of(new GeminiPart(text)));
    }
}