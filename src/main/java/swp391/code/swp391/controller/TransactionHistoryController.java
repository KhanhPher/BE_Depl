package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.Transaction;
import swp391.code.swp391.service.TransactionHistoryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    /**
     * Lấy danh sách lịch sử giao dịch
     * GET /api/transactions/history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(required = false) Transaction.PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Long stationId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        try {
            log.info("API: Lấy lịch sử giao dịch - userId: {}, status: {}", userId, status);

            TransactionFilterRequest filter = TransactionFilterRequest.builder()
                    .userId(userId)
                    .status(status)
                    .paymentMethod(paymentMethod)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .stationId(stationId)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();

            TransactionHistoryResponse response = transactionHistoryService.getTransactionHistory(filter);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Lấy lịch sử giao dịch thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Lấy summary (thống kê) cho giao dịch theo filter
     * GET /api/transactions/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getTransactionSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(required = false) Transaction.PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Long stationId
    ) {
        try {
            log.info("API: Lấy summary giao dịch - userId: {}, status: {}", userId, status);

            TransactionFilterRequest filter = TransactionFilterRequest.builder()
                    .userId(userId)
                    .status(status)
                    .paymentMethod(paymentMethod)
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .stationId(stationId)
                    .build();

            TransactionSummaryDTO summary = transactionHistoryService.getTransactionSummary(filter);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", summary);
            result.put("message", "Lấy summary giao dịch thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi lấy summary giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Lấy chi tiết một giao dịch
     * GET /api/transactions/{transactionId}
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransactionDetail(
            @PathVariable Long transactionId,
            @RequestParam(required = false) Long userId
    ) {
        try {
            log.info("API: Lấy chi tiết giao dịch: {}", transactionId);

            TransactionDetailDTO detail = transactionHistoryService.getTransactionDetail(transactionId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", detail);
            response.put("message", "Lấy chi tiết giao dịch thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Lấy lịch sử giao dịch của một user cụ thể
     * GET /api/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTransactionHistory(
            @PathVariable Long userId
    ) {
        try {
            log.info("API: Lấy lịch sử giao dịch của user: {}", userId);

            TransactionHistoryResponse response = transactionHistoryService.getUserTransactionHistory(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Lấy lịch sử giao dịch thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử giao dịch user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }
}