package swp391.code.swp391.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderDTO {

    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    private String reason;
}