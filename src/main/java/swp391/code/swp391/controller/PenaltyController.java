package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.FeeDetailDTO;
import swp391.code.swp391.dto.RetryPaymentRequestDTO;
import swp391.code.swp391.entity.Fee;
import swp391.code.swp391.entity.Transaction;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.TransactionRepository;
import swp391.code.swp391.repository.UserRepository;
import swp391.code.swp391.service.PaymentService;
import swp391.code.swp391.service.PenaltyService;
import swp391.code.swp391.service.PenaltyServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller cho Penalty/Fee Management
 *
 * Endpoints:
 * - GET /api/penalties/user/{userId}/history - AC6: Xem lịch sử phí của user
 * - GET /api/penalties/session/{sessionId}/details - AC6: Xem chi tiết phí của session
 * - GET /api/penalties/user/{userId}/unpaid - Lấy fees chưa thanh toán
 * - POST /api/penalties/cancel/{orderId} - AC1: Hủy order (có thể có phí)
 * - GET /api/penalties/session/{sessionId}/total - AC4: Tính tổng số tiền thanh toán
 */
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
@Slf4j
public class PenaltyController {

    private final PenaltyService penaltyService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    /**
     * AC6: Lấy lịch sử phí phạt của user
     * Hiển thị: loại phí, số tiền, lý do, thời gian
     */
    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<FeeDetailDTO>>> getUserFeeHistory(@PathVariable Long userId) {
        try {
            log.info("Getting fee history for user {}", userId);
            List<FeeDetailDTO> feeHistory = penaltyService.getUserFeeHistory(userId);

            return ResponseEntity.ok(APIResponse.success(
                    String.format("Tìm thấy %d phí phạt", feeHistory.size()),
                    feeHistory
            ));

        } catch (Exception e) {
            log.error("Error getting user fee history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi lấy lịch sử phí: " + e.getMessage()));
        }
    }

    /**
     * AC6: Lấy chi tiết phí của session
     */
    @GetMapping("/session/{sessionId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'STAFF')")
    public ResponseEntity<APIResponse<List<FeeDetailDTO>>> getSessionFeeDetails(@PathVariable Long sessionId) {
        try {
            log.info("Getting fee details for session {}", sessionId);
            List<FeeDetailDTO> feeDetails = penaltyService.getSessionFeeDetails(sessionId);

            return ResponseEntity.ok(APIResponse.success(
                    String.format("Tìm thấy %d phí phạt", feeDetails.size()),
                    feeDetails
            ));

        } catch (Exception e) {
            log.error("Error getting session fee details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi lấy chi tiết phí: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách transactions FAILED/PENDING của user
     * CHỈ trả về transactions thực sự cần xử lý
     */
    @GetMapping("/user/{userId}/unpaid")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Map<String, Object>>> getUnpaidFees(@PathVariable Long userId) {
        try {
            log.info("Getting unpaid transactions for user {}", userId);

            // Validate user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

            // Lấy tất cả transactions của user
            List<Transaction> allTransactions = transactionRepository.findByUserOrderByTransactionIdDesc(user);

            // Filter FAILED transactions - CẦN RETRY PAYMENT
            List<Transaction> failedTransactions = allTransactions.stream()
                    .filter(t -> t.getStatus() == Transaction.Status.FAILED || t.getStatus() == Transaction.Status.PENDING)
                    .filter(t -> t.getSession() != null) // Chỉ lấy transactions có session
                    .toList();

            List<Long> failedTransactionIds = failedTransactions.stream()
                    .map(Transaction::getTransactionId)
                    .collect(Collectors.toList());

            double totalFailedAmount = failedTransactions.stream()
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Filter PENDING transactions - ĐANG CHỜ VNPAY CALLBACK
            List<Transaction> pendingTransactions = allTransactions.stream()
                    .filter(t -> t.getStatus() == Transaction.Status.PENDING)
                    .filter(t -> t.getSession() != null) // Chỉ lấy transactions có session
                    .toList();

            List<Long> pendingTransactionIds = pendingTransactions.stream()
                    .map(Transaction::getTransactionId)
                    .collect(Collectors.toList());

            double totalPendingAmount = pendingTransactions.stream()
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Tổng hợp response - CHỈ TRANSACTIONS
            Map<String, Object> data = new HashMap<>();
            data.put("failedTransactionIds", failedTransactionIds);
            data.put("totalFailedTransactions", failedTransactions.size());
            data.put("totalFailedAmount", totalFailedAmount);
            data.put("pendingTransactionIds", pendingTransactionIds);
            data.put("totalPendingTransactions", pendingTransactions.size());
            data.put("totalPendingAmount", totalPendingAmount);

            log.info("User {} - Failed Txs: {}, Pending Txs: {}",
                    userId, failedTransactions.size(), pendingTransactions.size());

            return ResponseEntity.ok(APIResponse.success(
                    String.format("%d transaction thất bại (%,.0f VNĐ), %d transaction đang chờ (%,.0f VNĐ)",
                            failedTransactions.size(), totalFailedAmount,
                            pendingTransactions.size(), totalPendingAmount),
                    data
            ));

        } catch (Exception e) {
            log.error("Error getting unpaid transactions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi lấy transactions: " + e.getMessage()));
        }
    }

    /**
     * AC1: Hủy order - có thể bị tính phí nếu hủy muộn
     *
     * Request body: { "reason": "Lý do hủy" }
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<APIResponse<Map<String, Object>>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String reason = requestBody.getOrDefault("reason", "Không có lý do");

            // TODO: Extract userId from JWT
            // Long userId = jwtUtil.getUserIdFromToken(authHeader);
            Long userId = 1L; // Placeholder

            log.info("User {} canceling order {} with reason: {}", userId, orderId, reason);

            Fee cancelFee = penaltyService.handleLateCancellation(orderId, userId, reason);

            if (cancelFee != null) {
                // Có phí hủy muộn
                return ResponseEntity.ok(APIResponse.success(
                        String.format("Đã hủy lịch. Phí hủy muộn: %,.0f VNĐ", cancelFee.getAmount()),
                        Map.of(
                                "canceled", true,
                                "hasLateFee", true,
                                "feeAmount", cancelFee.getAmount(),
                                "feeDescription", cancelFee.getDescription()
                        )
                ));
            } else {
                // Hủy bình thường, không có phí
                return ResponseEntity.ok(APIResponse.success(
                        "Đã hủy lịch thành công",
                        Map.of(
                                "canceled", true,
                                "hasLateFee", false
                        )
                ));
            }

        } catch (Exception e) {
            log.error("Error canceling order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi hủy lịch: " + e.getMessage()));
        }
    }

    /**
     * AC4: Tính tổng số tiền thanh toán (base cost + fees)
     */
    @GetMapping("/session/{sessionId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'STAFF')")
    public ResponseEntity<APIResponse<Map<String, Object>>> calculateTotalPayment(@PathVariable Long sessionId) {
        try {
            log.info("Calculating total payment for session {}", sessionId);

            Double totalAmount = penaltyService.calculateTotalPaymentAmount(sessionId);
            List<FeeDetailDTO> feeDetails = penaltyService.getSessionFeeDetails(sessionId);

            Double totalFees = feeDetails.stream()
                    .mapToDouble(FeeDetailDTO::getAmount)
                    .sum();

            return ResponseEntity.ok(APIResponse.success(
                    "Tính toán thành công",
                    Map.of(
                            "totalAmount", totalAmount,
                            "baseCost", totalAmount - totalFees,
                            "totalFees", totalFees,
                            "feeCount", feeDetails.size(),
                            "fees", feeDetails
                    )
            ));

        } catch (Exception e) {
            log.error("Error calculating total payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi tính tổng thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Admin: Trigger manual no-show check for testing
     */
    @PostMapping("/admin/no-show/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Fee>> triggerNoShow(@PathVariable Long orderId) {
        try {
            log.info("Admin triggering no-show for order {}", orderId);
            Fee noShowFee = penaltyService.handleNoShow(orderId);

            if (noShowFee != null) {
                return ResponseEntity.ok(APIResponse.success(
                        "No-show processed successfully",
                        noShowFee
                ));
            } else {
                return ResponseEntity.ok(APIResponse.success(
                        "No-show not applicable",
                        null
                ));
            }

        } catch (Exception e) {
            log.error("Error triggering no-show: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi xử lý no-show: " + e.getMessage()));
        }
    }

    /**
     * Admin: Xem tất cả unpaid fees trong hệ thống
     */
    @GetMapping("/admin/unpaid-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<Fee>>> getAllUnpaidFees() {
        try {
            log.info("Admin getting all unpaid fees");
            // TODO: Implement getAllUnpaidFees in service
            return ResponseEntity.ok(APIResponse.success("Feature in development", null));

        } catch (Exception e) {
            log.error("Error getting all unpaid fees: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Thanh toán TẤT CẢ transactions chưa thanh toán và reset user về trạng thái bình thường
     * POST /api/penalties/pay-and-unlock
     *
     * Request body: {
     *   "userId": 456,
     *   "paymentMethod": "CASH"  // Optional, default CASH
     * }
     *
     * Logic:
     * 1. Tự động thanh toán TẤT CẢ FAILED + PENDING transactions
     * 2. Reset user.status = ACTIVE
     * 3. Reset user.violations = 0
     */
    @PostMapping("/pay-and-unlock")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Map<String, Object>>> payAndUnlock(
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            log.info("📥 Received pay-and-unlock request: {}", requestBody);

            // Validate required fields
            if (requestBody.get("userId") == null) {
                return ResponseEntity.badRequest()
                        .body(APIResponse.error("userId is required"));
            }

            Long userId = Long.valueOf(requestBody.get("userId").toString());

            // Get payment method (default to CASH)
            String paymentMethodStr = requestBody.getOrDefault("paymentMethod", "CASH").toString();
            Transaction.PaymentMethod paymentMethod;
            try {
                paymentMethod = Transaction.PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                paymentMethod = Transaction.PaymentMethod.CASH;
            }

            // Validate user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

            log.info("User {} attempting to pay all unpaid transactions with {} and unlock account",
                    userId, paymentMethod);

            // ===== BƯỚC 1: LẤY TẤT CẢ UNPAID TRANSACTIONS =====
            List<Transaction> unpaidTransactions = transactionRepository
                    .findByUserOrderByTransactionIdDesc(user)
                    .stream()
                    .filter(t -> t.getStatus() == Transaction.Status.FAILED ||
                                 t.getStatus() == Transaction.Status.PENDING)
                    .filter(t -> t.getSession() != null)
                    .toList();

            if (unpaidTransactions.isEmpty()) {
                // Không có transaction nào cần thanh toán - Vẫn reset user
                user.setStatus(User.UserStatus.ACTIVE);
                user.setViolations(0);
                String currentReason = user.getReasonReport() != null ? user.getReasonReport() : "";
                user.setReasonReport(currentReason +
                        "\n[Mở khóa: " + java.time.LocalDateTime.now() +
                        "] Tài khoản được mở khóa và reset violations (không có transaction cần thanh toán)");
                userRepository.save(user);

                return ResponseEntity.ok(APIResponse.success(
                        "Không có giao dịch nào cần thanh toán. Tài khoản đã được mở khóa!",
                        Map.of(
                                "unlocked", true,
                                "userStatus", "ACTIVE",
                                "violations", 0,
                                "paidTransactions", 0,
                                "totalAmount", 0.0
                        )
                ));
            }

            // ===== BƯỚC 2: THANH TOÁN TẤT CẢ TRANSACTIONS =====
            List<Long> paidTransactionIds = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            double totalPaid = 0.0;

            for (Transaction tx : unpaidTransactions) {
                try {
                    log.info("Retrying payment for transaction #{} (status: {})",
                            tx.getTransactionId(), tx.getStatus());

                    RetryPaymentRequestDTO retryRequest = RetryPaymentRequestDTO.builder()
                            .transactionId(tx.getTransactionId())
                            .userId(userId)
                            .paymentMethod(paymentMethod)
                            .build();

                    paymentService.retryPayment(retryRequest);
                    paidTransactionIds.add(tx.getTransactionId());
                    totalPaid += tx.getAmount();

                    log.info("✅ Successfully paid transaction #{}", tx.getTransactionId());

                } catch (Exception e) {
                    log.error("❌ Failed to pay transaction #{}: {}",
                            tx.getTransactionId(), e.getMessage());
                    errors.add("Transaction #" + tx.getTransactionId() + ": " + e.getMessage());
                }
            }

            // ===== BƯỚC 3: RESET USER STATUS & VIOLATIONS =====
            user.setStatus(User.UserStatus.ACTIVE);
            user.setViolations(0);
            String currentReason = user.getReasonReport() != null ? user.getReasonReport() : "";
            user.setReasonReport(currentReason +
                    "\n[Mở khóa: " + java.time.LocalDateTime.now() +
                    "] Tài khoản được mở khóa và reset violations sau khi thanh toán " +
                    paidTransactionIds.size() + " giao dịch");
            userRepository.save(user);

            log.info("✅ User {} unlocked: status=ACTIVE, violations=0", userId);

            // ===== RESPONSE =====
            String message;
            if (errors.isEmpty()) {
                message = String.format("✅ Đã thanh toán thành công %d giao dịch (tổng: %,.0f VNĐ). Tài khoản đã được mở khóa!",
                        paidTransactionIds.size(), totalPaid);
            } else {
                message = String.format("⚠️ Đã thanh toán %d/%d giao dịch. Tài khoản đã được mở khóa!",
                        paidTransactionIds.size(), unpaidTransactions.size());
            }

            return ResponseEntity.ok(APIResponse.success(
                    message,
                    Map.of(
                            "unlocked", true,
                            "userStatus", "ACTIVE",
                            "violations", 0,
                            "totalTransactions", unpaidTransactions.size(),
                            "paidTransactions", paidTransactionIds.size(),
                            "failedPayments", errors.size(),
                            "totalAmount", totalPaid,
                            "paidTransactionIds", paidTransactionIds,
                            "errors", errors,
                            "paymentMethod", paymentMethod.toString()
                    )
            ));

        } catch (Exception e) {
            log.error("Error in pay-and-unlock: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra user có thể mở khóa không
     * GET /api/penalties/user/{userId}/can-unlock
     */
    @GetMapping("/user/{userId}/can-unlock")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Boolean>> canUnlockUser(@PathVariable Long userId) {
        try {
            log.info("Checking if user {} can be unlocked", userId);
            boolean canUnlock = penaltyService.canUnlockUser(userId);

            String message = canUnlock
                    ? "User có thể mở khóa (đã thanh toán hết phí)"
                    : "User chưa thể mở khóa (còn phí chưa thanh toán hoặc không bị banned)";

            return ResponseEntity.ok(APIResponse.success(message, canUnlock));

        } catch (Exception e) {
            log.error("Error checking unlock status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy tổng hợp tất cả transactions chưa thanh toán (FAILED/PENDING)
     * Để FE hiển thị tổng số tiền trước khi thanh toán
     * GET /api/penalties/user/{userId}/unpaid-summary
     */
    @GetMapping("/user/{userId}/unpaid-summary")
    @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<APIResponse<PenaltyServiceImpl.UnpaidTransactionsSummary>> getUnpaidTransactionsSummary(
            @PathVariable Long userId) {
        try {
            log.info("Getting unpaid transactions summary for user {}", userId);

            PenaltyServiceImpl.UnpaidTransactionsSummary summary =
                penaltyService.getUnpaidTransactionsSummary(userId);

            String message = summary.transactionIds.isEmpty()
                ? "Không có giao dịch nào cần thanh toán"
                : String.format("Bạn có %d giao dịch chưa thanh toán, tổng: %,.0f VNĐ",
                    summary.transactionIds.size(), summary.totalAmount);

            return ResponseEntity.ok(APIResponse.success(message, summary));

        } catch (Exception e) {
            log.error("Error getting unpaid transactions summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Lỗi khi lấy thông tin giao dịch chưa thanh toán: " + e.getMessage()));
        }
    }
}
