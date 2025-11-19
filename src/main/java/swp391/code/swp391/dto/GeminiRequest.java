package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<GeminiContent> contents;

    @JsonProperty("system_instruction")
    private GeminiSystemInstruction systemInstruction;

    /**
     * Hàm static "from" (để các hàm stateless cũ có thể gọi)
     * Nó gói một chuỗi text thành một Request hoàn chỉnh.
     */
    public static GeminiRequest from(String text) {
        // Tạo nội dung với role "user", sử dụng cấu trúc đúng
        List<GeminiPart> parts = List.of(new GeminiPart(text));
        GeminiContent content = new GeminiContent(parts, "user");
        return new GeminiRequest(List.of(content), null);
    }
}