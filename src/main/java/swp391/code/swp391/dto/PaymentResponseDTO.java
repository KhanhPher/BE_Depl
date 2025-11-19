package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private Long transactionId;
    private Long sessionId;
    private BigDecimal amount;
    private Transaction.PaymentMethod paymentMethod;
    private Transaction.Status status;
    private String message;

    // Dành cho VNPay
    private String paymentUrl;

    // Chi tiết giao dịch
    private LocalDateTime createdAt;

    // Chi tiết hóa đơn
    private PaymentDetailDTO paymentDetail;
}