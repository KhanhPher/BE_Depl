package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeChargingPointRequestDTO {

    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "Charging Point ID hiện tại không được để trống")
    private Long currentChargingPointId;

    @NotNull(message = "Charging Point ID mới không được để trống")
    private Long newChargingPointId;

    private String reason; // Lý do đổi trụ sạc

    private Long staffId; // ID của staff thực hiện
}