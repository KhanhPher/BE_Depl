package swp391.code.swp391.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Payment method is required")
    private Transaction.PaymentMethod paymentMethod;

    // Dành cho VNPay - URL để quay về sau khi thanh toán
    private String returnUrl;

    // Dành cho VNPay - mã ngân hàng (tùy chọn)
    private String bankCode;
}