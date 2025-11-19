package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Charging Session information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChargingSessionDTO {

    private Long sessionId;
    private Long userId;
    private String userName;
    private Long stationId;
    private String stationName;
    private Long chargingPointId;
    private String connectorType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double energyConsumed; // kWh
    private Double totalCost; // VND
    private String status; // ACTIVE, COMPLETED, CANCELLED
    private Integer duration; // minutes

    // Additional info
    private String vehicleModel;
    private String paymentMethod;
}

