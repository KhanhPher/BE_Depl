package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO này dùng để hứng dữ liệu JSON đã được bóc tách
 * từ phản hồi của người dùng (do Gemini thực hiện).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Bỏ qua nếu Gemini trả về key lạ
public class ParsedIssueReportDTO {

    /**
     * Tên trạm sạc mà Gemini tìm thấy.
     * Khớp với key "station_name" trong JSON.
     */
    @JsonProperty("station_name")
    private String stationName;

    /**
     * Loại trụ sạc mà Gemini tìm thấy.
     * Khớp với key "connector_type" trong JSON.
     */
    @JsonProperty("connector_type")
    private String connectorType;

    /**
     * Loại lỗi mà Gemini phân loại.
     * Khớp với key "issue_type" trong JSON.
     */
    @JsonProperty("issue_type")
    private String issueType;

    /*
    * Thông tin user
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * Cảm xúc của người dùng.
     * Khớp với key "user_sentiment" trong JSON.
     */
    @JsonProperty("user_sentiment")
    private String userSentiment;
}