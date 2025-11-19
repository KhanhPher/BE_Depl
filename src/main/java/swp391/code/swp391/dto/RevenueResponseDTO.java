package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO cho báo cáo doanh thu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueResponseDTO {

    // Tổng quan doanh thu
    private RevenueSummary summary;

    // Dữ liệu biểu đồ theo thời gian
    private List<RevenueChartData> chartData;

    // Doanh thu theo trạm
    private List<RevenueByStation> revenueByStation;

    // Doanh thu theo phương thức thanh toán
    private Map<String, BigDecimal> revenueByPaymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueSummary {
        private BigDecimal totalRevenue;
        private BigDecimal totalSuccessRevenue;
        private BigDecimal totalPendingRevenue;
        private BigDecimal totalFailedRevenue;
        private Long totalTransactions;
        private Long successfulTransactions;
        private Long pendingTransactions;
        private Long failedTransactions;
        private BigDecimal averageTransactionAmount;
        private BigDecimal growthRate; // % tăng trưởng so với kỳ trước
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChartData {
        private String period; // Ngày/Tuần/Tháng
        private LocalDateTime date;
        private BigDecimal revenue;
        private Long transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByStation {
        private Long stationId;
        private String stationName;
        private String stationAddress;
        private BigDecimal revenue;
        private Long transactionCount;
        private BigDecimal averagePerTransaction;
    }
}