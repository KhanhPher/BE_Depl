package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPaymentRequestDTO {
    private Long transactionId;
    private Long userId;
    private Transaction.PaymentMethod paymentMethod; // VNPAY, CASH, QR
    private String returnUrl; // Cho VNPay
    private String bankCode; // Cho VNPay (optional)
}