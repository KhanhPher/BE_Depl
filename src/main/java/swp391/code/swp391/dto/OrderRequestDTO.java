package swp391.code.swp391.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Vehicle ID không được để trống")
    private Long vehicleId;

    @NotNull(message = "Station ID không được để trống")
    private Long stationId;

    @NotNull(message = "Dung lượng pin hiện tại không được để trống")
    @DecimalMin(value = "0.0", message = "Pin hiện tại phải >= 0%")
    @DecimalMax(value = "100.0", message = "Pin hiện tại phải <= 100%")
    private Double currentBattery; // % pin hiện tại

    @NotNull(message = "Dung lượng pin mong muốn không được để trống")
    @DecimalMin(value = "0.0", message = "Pin mong muốn phải >= 0%")
    @DecimalMax(value = "100.0", message = "Pin mong muốn phải <= 100%")
    private Double targetBattery; // % pin mong muốn

    // Optional fields for staff-initiated sessions
    private Long staffId; // ID của staff tạo booking (nếu có)
    private Long chargingPointId; // ID của trụ sạc cụ thể (optional)

    // Optional: Ngày muốn tìm slot (mặc định là hôm nay)
    private String searchDate; // Format: "yyyy-MM-dd" (VD: "2025-11-10")
}