package swp391.code.swp391.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import swp391.code.swp391.dto.GeminiRequest;
import swp391.code.swp391.dto.GeminiResponse;
import java.time.Duration;

@Component
public class GeminiApiCaller {
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiCaller.class);
    @Value("${gemini.api.key}") private String apiKey;
    @Value("${gemini.api.model:gemini-1.0-pro}") private String modelName;
    private final WebClient webClient;
    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/{modelName}:generateContent";

    public GeminiApiCaller(WebClient geminiWebClient) {
        this.webClient = geminiWebClient;
    }

    public String callGemini(GeminiRequest requestBody) {
        // KHÔNG CẦN DÒNG NÀY NỮA: GeminiRequest requestBody = GeminiRequest.from(promptText);

        try {
            logger.info("Gọi Gemini API (Model: {})...", modelName);

            GeminiResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GENERATE_CONTENT_PATH)
                            .queryParam("key", apiKey)
                            .build(modelName))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody) // <-- Gửi thẳng object request
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .retryWhen(
                            Retry.backoff(2, Duration.ofSeconds(1))
                                    .filter(ex -> ex instanceof WebClientResponseException wcre && wcre.getStatusCode().is5xxServerError())
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                            new RuntimeException("Retry API thất bại (lỗi server)", retrySignal.failure()))
                    )
                    .block();

            if (response != null) {
                String txt = response.getFirstTextResponse();
                logger.info("Nhận phản hồi từ Gemini");
                return txt != null ? txt.replace("\\n", "\n") : null;
            }
            logger.warn("Không có response từ Gemini (response body rỗng)");
            return null;
        } catch (WebClientResponseException wcre) {
            logger.warn("Lỗi Client khi gọi Gemini API ({}): {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Lỗi hệ thống khi gọi Gemini API: {}", e.getMessage(), e);
            return null;
        }
    }
}