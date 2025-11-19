package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Transaction;

import java.time.LocalDateTime;

/**
 * DTO cho việc filter báo cáo doanh thu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueFilterRequestDTO {

    // Filter theo khoảng thời gian
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    // Filter theo trạm sạc
    private Long stationId;

    // Filter theo phương thức thanh toán
    private Transaction.PaymentMethod paymentMethod;

    // Filter theo trạng thái giao dịch
    private Transaction.Status status;

    // Group by (DAY, WEEK, MONTH, YEAR)
    private String groupBy;

    // Export format (PDF, EXCEL)
    private String exportFormat;
}