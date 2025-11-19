package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO để hiển thị danh sách phiên sạc cho Staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListDTO {
    private Long sessionId;
    private Long orderId;
    private Long userId;
    private String userName;
    private String userPhone;
    private Long chargingPointId;
    private String connectorType;
    private Double powerOutput;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double powerConsumed; // kWh đã sạc
    private Double baseCost; // Tiền cơ bản
    private String status; // CHARGING, COMPLETED, OVERTIME
    private Boolean isOvertime; // Có quá giờ không
    private Long overtimeMinutes; // Số phút quá giờ
}

