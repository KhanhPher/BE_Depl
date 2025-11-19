// java
package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.CarModel;
import swp391.code.swp391.entity.ConnectorType;
import swp391.code.swp391.entity.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO {
    private String plateNumber;
    private CarModelDTO carModel;
    private Long userId;
    private Long vehicleId;

    // Constructor from Vehicle entity
    public VehicleResponseDTO(Vehicle vehicle) {
        if (vehicle == null) return;
        this.vehicleId = vehicle.getId(); // Set vehicleId from entity
        this.plateNumber = vehicle.getPlateNumber();
        this.vehicleId = vehicle.getId();
        if (vehicle.getCarModel() != null) {
            CarModel cm = vehicle.getCarModel();
            List<Long> connectorTypeIds = null;
            if (cm.getConnectorTypes() != null) {
                connectorTypeIds = cm.getConnectorTypes().stream()
                        .map(ConnectorType::getConnectorTypeId)
                        .collect(Collectors.toList());
            }

            this.carModel = new CarModelDTO(
                    cm.getCar_model_id(),
                    cm.getBrand(),
                    cm.getModel(),
                    cm.getCapacity(),
                    cm.getProductYear(),
                    connectorTypeIds,
                    cm.getImg_url()
            );
        }

        if (vehicle.getUser() != null) {
            this.userId = vehicle.getUser().getUserId();
        }
    }
}