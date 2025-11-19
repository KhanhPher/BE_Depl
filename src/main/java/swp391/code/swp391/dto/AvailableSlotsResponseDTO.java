package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response chính cho API tìm các trạm và charging point khả dụng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsResponseDTO {

    private Long stationId;
    private String stationName;
    private String address;
    private Double latitude;
    private Double longitude;

    private VehicleInfo vehicleInfo;
    private ChargingInfo chargingInfo;

    // Danh sách charging points có connector tương thích VÀ có gaps đủ thời gian
    private List<ChargingPointAvailabilityDTO> chargingPoints;

    // danh sách slot khả dụng của 1 point
    private List<AvailableTimeSlotDTO> availableSlots;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleInfo {
        private Long vehicleId;
        private String brand;
        private String model;
        private Double batteryCapacity; // kWh
        private List<String> compatibleConnectors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargingInfo {
        private Double currentBattery; // %
        private Double targetBattery;  // %
        private Double batteryToCharge; // %
        private Double energyToCharge;  // kWh cần sạc
    }
}
