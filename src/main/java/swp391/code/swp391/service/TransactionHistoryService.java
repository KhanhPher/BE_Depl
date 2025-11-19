package swp391.code.swp391.service;

import swp391.code.swp391.dto.TransactionDetailDTO;
import swp391.code.swp391.dto.TransactionFilterRequest;
import swp391.code.swp391.dto.TransactionHistoryResponse;
import swp391.code.swp391.dto.TransactionSummaryDTO;

public interface TransactionHistoryService {

    /**
     * Lấy danh sách lịch sử giao dịch
     */
    TransactionHistoryResponse getTransactionHistory(TransactionFilterRequest filter);

    /**
     * Lấy chi tiết một giao dịch cụ thể
     */
    TransactionDetailDTO getTransactionDetail(Long transactionId, Long userId);

    /**
     * Lấy lịch sử giao dịch của một user
     */
    TransactionHistoryResponse getUserTransactionHistory(Long userId);

    /**
     * Lấy thống kê tổng quan (summary) theo filter
     */
    TransactionSummaryDTO getTransactionSummary(TransactionFilterRequest filter);
}