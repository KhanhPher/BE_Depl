package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO chứa thống kê (summary) cho các giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryDTO {
    private Long totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal totalSuccess;
    private BigDecimal totalFailed;
    private BigDecimal totalPending;
}