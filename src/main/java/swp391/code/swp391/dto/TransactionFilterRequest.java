package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.time.LocalDateTime;

/**
 * DTO cho việc filter/search lịch sử giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFilterRequest {

    // Filter theo user (nếu admin/staff xem tất cả, driver chỉ xem của mình)
    private Long userId;

    // Filter theo status
    private Transaction.Status status;

    // Filter theo payment method
    private Transaction.PaymentMethod paymentMethod;

    // Filter theo khoảng thời gian
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    // Filter theo trạm sạc
    private Long stationId;

    // Sort
    private String sortBy; // Field để sort (ví dụ: "createdAt", "amount")
    private String sortDirection; // "ASC" hoặc "DESC"
}