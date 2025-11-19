package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.CarModel;
import swp391.code.swp391.entity.ChargingPoint;
import swp391.code.swp391.entity.Vehicle;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectorTypeDTO {

    private Long connectorTypeId;

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must not exceed 100 characters")
    private String typeName;

    @NotNull(message = "Power output is required")
    private Double powerOutput;

    @NotNull(message = "Price per kWh is required")
    private Double pricePerKwh;

    // Cho input: chỉ cần charging point ID
    private List<Long> chargingPointIds; // list of IDs to gán vào ConnectorType

    // Cho output: full ChargingPoint object
    private List<ChargingPoint> chargingPoints;

    // Cho output: list vehicles sử dụng connector type này
    private List<Vehicle> vehicles;


    public void setCarModel(List<CarModel> carModels) {
    }
}