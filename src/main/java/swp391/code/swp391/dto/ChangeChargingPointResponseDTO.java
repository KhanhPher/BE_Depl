package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeChargingPointResponseDTO {

    private Long orderId;
    private Long oldChargingPointId;
    private String oldChargingPointInfo;
    private Long newChargingPointId;
    private String newChargingPointInfo;
    private String driverName;
    private Long driverId;
    private String reason;
    private LocalDateTime changedAt;
    private String changedByStaff;
    private boolean notificationSent;
    private String message;
}