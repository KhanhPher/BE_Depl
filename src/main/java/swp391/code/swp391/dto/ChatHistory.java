package swp391.code.swp391.dto;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ChatHistory implements Serializable {

    private final List<ChatMessage> history = new ArrayList<>();

    public void addMessage(String role, String text) {
        this.history.add(new ChatMessage(role, text));
        // (Bạn có thể thêm logic giới hạn 50 tin nhắn ở đây)
    }

    public void clear() {
        this.history.clear();
    }
}