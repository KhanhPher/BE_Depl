package swp391.code.swp391.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data @JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiChatDecision {
    @JsonProperty("intent")
    private String intent; // ASKING_ABOUT_STATION, FIND_NEARBY_STATION, CHECK_AVAILABILITY, ASKING_QUESTION, REPORTING_ISSUE

    @JsonProperty("answer")
    private String chatResponse; // Câu trả lời từ AI

    @JsonProperty("report_details")
    private ParsedIssueReportDTO reportDetails; // Dùng cho REPORTING_ISSUE

    @JsonProperty("station_name")
    private String stationName; // Dùng cho ASKING_ABOUT_STATION, CHECK_AVAILABILITY

    @JsonProperty("hour")
    private Integer hour; // Dùng cho CHECK_AVAILABILITY

    // NEW: Dùng cho FIND_NEARBY_STATION
    @JsonProperty("location")
    private String location; // Tên địa điểm (VD: "Quận 1", "Thủ Đức")

    @JsonProperty("latitude")
    private Double latitude; // Vĩ độ

    @JsonProperty("longitude")
    private Double longitude; // Kinh độ

    // Sentiment analysis
    @JsonProperty("sentiment")
    private String sentiment; // POSITIVE, NEGATIVE, NEUTRAL

    // Confidence score
    @JsonProperty("confidence")
    private Double confidence; // 0.0 - 1.0
}