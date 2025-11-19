package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /**
     * "user" (người dùng) hoặc "model" (bot)
     */
    private String role;
    private String text;
}