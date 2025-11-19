package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho chi tiết đầy đủ của một giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailDTO {

    // Thông tin giao dịch
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
    private String userPhone;

    // Thông tin phiên sạc
    private Long sessionId;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private BigDecimal powerConsumed; // kWh
    private BigDecimal baseCost;

    // Thông tin trạm sạc
    private Long stationId;
    private String stationName;
    private String stationAddress;
    private String connectorTypeName;

    // Chi tiết tính giá
    private BigDecimal basePrice; // Giá/kWh
    private BigDecimal priceFactor; // Hệ số giờ cao điểm
    private BigDecimal subscriptionDiscount; // Giảm giá gói

    // Các khoản phí
    private List<FeeDetailDTO> fees;
    private BigDecimal totalFees;

    // Thông tin VNPay
    private String vnpayTransactionNo;
    private String vnpayBankCode;
    private String vnpayCardType;

    // Thông tin xe
    private String vehiclePlateNumber;
    private String vehicleBrand;
    private String vehicleModel;
}