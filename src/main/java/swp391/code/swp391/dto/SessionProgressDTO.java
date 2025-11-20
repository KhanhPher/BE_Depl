package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionProgressDTO {

    // Battery info
    private Double startBattery; // Pin ban đầu (%)
    private Double currentBattery; // Pin hiện tại (%)
    private Double targetBattery; // Pin mong đợi/mục tiêu (%)
    private Double progressPercentage; // % hoàn thành = (current - start) / (target - start) * 100

    // Power & Cost
    private Double powerConsumed; // Điện năng đã tiêu thụ (kWh)
    private Double cost; // Chi phí hiện tại (VND)

    // Time tracking
    private Long elapsedSeconds; // Thời gian đã sạc (giây)
    private Long elapsedMinutes; // Thời gian đã sạc (phút) - backward compatibility
    private Long estimatedRemainingMinutes; // Thời gian còn lại ước tính (phút)
    private LocalDateTime startTime; // Thời điểm bắt đầu
    private LocalDateTime currentTime; // Thời điểm hiện tại
    
    // Session status
    private String status; // Session status: "CHARGING", "PARKING", "COMPLETED"
}
