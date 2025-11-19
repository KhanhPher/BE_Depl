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
public class ConflictingOrderDTO {
    private Long orderId;
    private Long chargingPointId;
    private String chargingPointName;
    private String driverName;
    private String driverEmail;
    private String driverPhone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String vehiclePlate;
    private String connectorType;

    // Thông tin conflict
    private Long conflictWithOrderId;
    private String conflictType; // "OVERLAP", "BACK_TO_BACK", "LATE_CHECKOUT"
    private String conflictDescription;
    private Integer overlapMinutes;
}