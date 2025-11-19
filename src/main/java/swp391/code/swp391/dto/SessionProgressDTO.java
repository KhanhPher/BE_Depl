package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionProgressDTO {

    private Double currentBattery; //(%)
    private Double powerConsumed; //(kWh)
    private Double cost; //(VND)
    private Long elapsedSeconds; // Thời gian đã sạc (giây) - Thay đổi từ elapsedMinutes
    private Long elapsedMinutes; // Thời gian đã sạc (phút) - Giữ lại để backward compatibility
    private Long estimatedRemainingMinutes; // Thời gian còn lại ước tính (phút)
    private LocalDateTime startTime; // Thời điểm bắt đầu
    private LocalDateTime currentTime; // Thời điểm hiện tại
}
