package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.ChargingPoint.ChargingPointStatus;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.ConnectorType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChargingPointDTO {

    private Long chargingPointId;

    private String chargingPointName; // Số thứ tự/tên của charging point (VD: "A1", "A2", "B1")

    @NotNull(message = "Type name is required")
    private String typeName;

    @NotNull(message = "Status is required")
    private ChargingPointStatus status;

    private Long connectorTypeId;

    // Cho input: chỉ cần station ID
    private Long stationId;

    // Cho output: full ChargingStation object
    private ChargingStation station;

    // Cho output: list connector types
    private ConnectorType connectorType;

    private double powerOutput;
    private double pricePerKwh;
}