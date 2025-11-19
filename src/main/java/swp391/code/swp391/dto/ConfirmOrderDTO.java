package swp391.code.swp391.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmOrderDTO {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Vehicle ID không được để trống")
    private Long vehicleId;

    @NotNull(message = "Station ID không được để trống")
    private Long stationId;

    @NotNull(message = "Charging Point ID không được để trống")
    private Long chargingPointId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    @NotNull(message = "Pin hiện tại không được để trống")
    private Double currentBattery;

    @NotNull(message = "Pin mong muốn không được để trống")
    private Double targetBattery;

    @NotNull(message = "Năng lượng cần sạc không được để trống")
    private Double energyToCharge; // kWh

    @NotNull(message = "Chi phí dự kiến không được để trống")
    private Double estimatedCost; // VND

    private String notes;

    @NotNull(message = "Loại kết nối không được để trống")
    private Long connectorTypeId;

    // Danh sách slot IDs người dùng chọn (ví dụ: FIXED_02:00_04:00, MINI_09:30_10:45, STAFF_10:00_11:00)
    private List<String> slotIds;
}