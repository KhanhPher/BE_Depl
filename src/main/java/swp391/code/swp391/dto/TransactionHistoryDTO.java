package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho việc hiển thị danh sách lịch sử giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryDTO {

    // Thông tin giao dịch cơ bản
    private Long transactionId;
    private BigDecimal amount;
    private Transaction.PaymentMethod paymentMethod;
    private Transaction.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime paymentTime;

    // Thông tin người dùng
    private Long userId;
    private String userName;
    private String userEmail;

    // Thông tin phiên sạc
    private Long sessionId;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private BigDecimal powerConsumed; // kWh

    // Thông tin trạm sạc
    private String stationName;
    private String stationAddress;

    // Thông tin VNPay
    private String vnpayTransactionNo;
    private String vnpayBankCode;
}