package swp391.code.swp391.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Session;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class SessionDTO {

    private Long sessionId;
    private Long chargingPointId;
    private Long orderId;
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentBattery;
    private double expectedBattery;
    private Long connectorTypeId;
    private Session.SessionStatus status; // Status của session
    private LocalDateTime parkingStartTime; // Thời gian bắt đầu tính phí đỗ xe
}
