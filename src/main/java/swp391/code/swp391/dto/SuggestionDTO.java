package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về cho API /connector-suggestions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionDTO {
    private String stationName;
    private String connectorType;
    private String suggestionMessage; // Ví dụ: "NÊN THÊM: Trụ đang quá tải..."
    private double utilizationRate; // Tỷ lệ sử dụng (ví dụ: 0.85)
}