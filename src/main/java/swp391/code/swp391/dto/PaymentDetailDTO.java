package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetailDTO {

    // Thông tin người dùng
    private String userName;
    private String userEmail;

    // Thông tin trạm sạc
    private String stationName;
    private String stationAddress;

    // Thông tin phiên sạc
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private BigDecimal powerConsumed; // kWh

    // Chi tiết chi phí
    private BigDecimal basePrice; // Giá mỗi kWh từ ConnectorType
    // ĐÃ XÓA: priceFactor - không còn tính theo hệ số giờ cao điểm
    private BigDecimal subscriptionDiscount; // Phần trăm giảm giá
    private BigDecimal baseCost; // Chi phí cơ bản của phiên sạc

    // Các khoản phí
    private List<FeeDetailDTO> fees;
    private BigDecimal totalFees;

    // Tổng số tiền
    private BigDecimal totalAmount;

    // Thông tin thanh toán
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paymentTime;
}