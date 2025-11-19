package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleChargingStatusDTO {

    private Long vehicleId;

    private Boolean isCurrentlyCharging;

    private Boolean canBookNow;

    private String message;
}

