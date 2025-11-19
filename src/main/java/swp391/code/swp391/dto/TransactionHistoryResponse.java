package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response cho danh sách lịch sử giao dịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponse {

    // Danh sách giao dịch (toàn bộ kết quả theo filter)
    private List<TransactionHistoryDTO> transactions;

    // Tổng phần tử trả về
    private Integer totalElements;
}