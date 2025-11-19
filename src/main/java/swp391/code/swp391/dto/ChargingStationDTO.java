package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.ChargingStation.ChargingStationStatus;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChargingStationDTO {

    //@NotNull(message = "Station ID is required")
    private Long stationId;

    @NotBlank(message = "Station name is required")
    @Size(max = 255, message = "Station name must not exceed 255 characters")
    private String stationName;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Status is required")
    private ChargingStationStatus status;

    private Double latitude;
    private Double longitude;

    // Field lưu khoảng cách (km) từ vị trí user đến trạm (dùng cho findNearbyStations)
    private Double distance;

    @NotNull(message = "At least one charging point is required")
    private List<ChargingPointDTO> chargingPoints;

    @NotNull(message = "Number of charging points is required")
    private int chargingPointNumber;

    // Field chứa thông tin tiện ích xung quanh (được tạo bởi AI)
    private String surroundingAmenities;

    // Số lượng order của trạm
    private Long orderCount;
}