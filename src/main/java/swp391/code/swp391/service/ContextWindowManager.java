package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * CONTEXT WINDOW MANAGER
 * Quản lý số lượng tin nhắn trong lịch sử để tránh vượt quá giới hạn token của Gemini
 */
@Service
@RequiredArgsConstructor
public class ContextWindowManager {

    private static final Logger logger = LoggerFactory.getLogger(ContextWindowManager.class);

    // Giới hạn Gemini Pro: ~30,000 tokens
    // 1 tin nhắn trung bình ~100-200 tokens
    private static final int MAX_MESSAGES = 50; // Giữ tối đa 50 tin nhắn (an toàn)
    private static final int SUMMARY_THRESHOLD = 40; // Khi đạt 40 tin nhắn, tóm tắt

    private final GeminiApiCaller geminiApiCaller;

    /**
     * Tối ưu hóa lịch sử chat để không vượt quá giới hạn token
     * Strategy:
     * 1. Nếu < 40 tin nhắn: Giữ nguyên
     * 2. Nếu 40-50: Tóm tắt 20 tin nhắn đầu thành 1 tin nhắn
     * 3. Nếu > 50: Xóa tin nhắn cũ nhất, giữ 50 tin nhắn gần nhất
     */
    public List<ChatMessage> optimizeHistory(List<ChatMessage> history) {
        if (history.size() <= SUMMARY_THRESHOLD) {
            return history; // Không cần tối ưu
        }

        logger.info("Optimizing chat history: {} messages", history.size());

        if (history.size() > MAX_MESSAGES) {
            // Strategy 3: Cắt bỏ tin nhắn cũ
            logger.warn("History exceeded MAX_MESSAGES ({}), truncating...", MAX_MESSAGES);
            return new ArrayList<>(history.subList(history.size() - MAX_MESSAGES, history.size()));
        }

        // Strategy 2: Tóm tắt tin nhắn cũ
        try {
            List<ChatMessage> oldMessages = history.subList(0, 20);
            List<ChatMessage> recentMessages = history.subList(20, history.size());

            String summary = summarizeOldMessages(oldMessages);

            List<ChatMessage> optimized = new ArrayList<>();
            optimized.add(new ChatMessage("user", "[TÓM TẮT HỘI THOẠI TRƯỚC]: " + summary));
            optimized.addAll(recentMessages);

            logger.info("Summarized {} old messages into 1 summary", oldMessages.size());
            return optimized;

        } catch (Exception e) {
            logger.error("Failed to summarize history, using truncation instead", e);
            return new ArrayList<>(history.subList(history.size() - MAX_MESSAGES, history.size()));
        }
    }

    /**
     * Tóm tắt các tin nhắn cũ bằng Gemini
     */
    private String summarizeOldMessages(List<ChatMessage> messages) {
        StringBuilder conversation = new StringBuilder();
        for (ChatMessage msg : messages) {
            conversation.append(msg.getRole()).append(": ").append(msg.getText()).append("\n");
        }

        String prompt = String.format(
            """
            Hãy tóm tắt đoạn hội thoại sau thành 2-3 câu ngắn gọn, giữ lại thông tin quan trọng:
            
            %s
            
            Chỉ trả về phần tóm tắt, không giải thích.
            """,
            conversation.toString()
        );

        // Gọi Gemini để tóm tắt (sử dụng model nhẹ hơn nếu có)
        swp391.code.swp391.dto.GeminiRequest request =
            swp391.code.swp391.dto.GeminiRequest.from(prompt);

        String summary = geminiApiCaller.callGemini(request);
        return summary != null ? summary : "[Không thể tóm tắt hội thoại cũ]";
    }

    /**
     * Tính toán ước lượng số token trong lịch sử
     * (Rough estimation: 1 token ≈ 4 characters)
     */
    public int estimateTokenCount(List<ChatMessage> history) {
        int totalChars = history.stream()
            .mapToInt(msg -> msg.getText().length())
            .sum();

        return totalChars / 4; // Ước lượng thô
    }
}

