package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPaymentResponseDTO {
    private Long transactionId;
    private Long sessionId;
    private BigDecimal amount;
    private Transaction.PaymentMethod paymentMethod;
    private Transaction.Status status;
    private String message;
    private String paymentUrl; // Cho VNPay
    private LocalDateTime createdAt;
    private PaymentDetailDTO paymentDetail;
}