package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.FeeDetailDTO;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.exception.ApiRequestException;
import swp391.code.swp391.repository.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của PenaltyService
 * Xử lý các loại phí phạt theo Acceptance Criteria
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyServiceImpl implements PenaltyService {

    private final FeeRepository feeRepository;
    private final OrderRepository orderRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FeeCalculationService feeCalculationService;
    private final NotificationService notificationService;

    // Cấu hình phí (AC7 - có thể move sang SystemConfig)
    private static final int LATE_CANCEL_MINUTES = 10; // Hủy muộn nếu < 10 phút
    private static final int NO_SHOW_GRACE_MINUTES = 30; // No-show nếu không đến sau 30 phút
    private static final int AUTO_LOCK_VIOLATIONS = 3; // Auto lock sau 3 vi phạm

    @Override
    @Transactional(rollbackFor = Exception.class) // AC8: Rollback nếu lỗi
    public Fee handleLateCancellation(Long orderId, Long userId, String reason) {
        log.info("AC1: Processing late cancellation for order {} by user {}", orderId, userId);

        try {
            // Validate order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ApiRequestException("Order không tồn tại"));

            if (!order.getUser().getUserId().equals(userId)) {
                throw new ApiRequestException("Bạn không có quyền hủy order này");
            }

            if (order.getStatus() != Order.Status.BOOKED) {
                throw new ApiRequestException("Order không ở trạng thái BOOKED");
            }

            // Kiểm tra thời gian hủy
            LocalDateTime now = LocalDateTime.now();
            Duration timeUntilStart = Duration.between(now, order.getStartTime());
            long minutesUntilStart = timeUntilStart.toMinutes();

            if (minutesUntilStart < LATE_CANCEL_MINUTES) {
                // Hủy muộn - tính phí 10%
                log.warn("Late cancellation detected: {} minutes before start", minutesUntilStart);

                // 1. Tạo fee (10% estimated cost)
                Fee cancelFee = feeCalculationService.calculateCancelFee(order);
                log.info("Created CANCEL fee: {} VNĐ for order {}", cancelFee.getAmount(), orderId);

                // 2. Cập nhật order status
                order.setStatus(Order.Status.CANCELED);
                order.setCanceledAt(now);
                order.setCancellationReason(reason);
                orderRepository.save(order);

                // 3. Tăng violation count
                incrementViolationCount(userId, "Hủy lịch muộn (< 10 phút): " + reason);

                // 4. Kiểm tra auto-lock
                boolean isLocked = checkAndAutoLockUser(userId);

                // 5. Gửi notification
                String additionalInfo = isLocked ? " Tài khoản bị khóa do vi phạm 3 lần." : "";
                notificationService.createPenaltyNotification(
                        orderId,
                        NotificationServiceImpl.PenaltyEvent.CANCEL_PENALTY,
                        cancelFee.getAmount(),
                        reason + additionalInfo
                );

                return cancelFee;

            } else {
                // Hủy bình thường - không tính phí
                log.info("Normal cancellation: {} minutes before start", minutesUntilStart);
                order.setStatus(Order.Status.CANCELED);
                order.setCanceledAt(now);
                order.setCancellationReason(reason);
                orderRepository.save(order);

//                notificationService.createGeneralNotification(
//                        List.of(userId),
//                        "Đã hủy lịch",
//                        "Lịch đặt của bạn đã được hủy thành công."
//                );

                return null; // Không có phí
            }

        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in handleLateCancellation: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi xử lý hủy lịch: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // AC8: Rollback nếu lỗi
    public Fee handleNoShow(Long orderId) {
        log.info("AC2: Processing no-show for order {}", orderId);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ApiRequestException("Order không tồn tại"));

            if (order.getStatus() != Order.Status.BOOKED) {
                log.warn("Order {} is not BOOKED, skipping no-show", orderId);
                return null;
            }

            // Kiểm tra đã quá 30 phút sau startTime
            LocalDateTime now = LocalDateTime.now();
            Duration timeSinceStart = Duration.between(order.getStartTime(), now);
            long minutesSinceStart = timeSinceStart.toMinutes();

            if (minutesSinceStart >= NO_SHOW_GRACE_MINUTES) {
                log.warn("No-show detected for order {}: {} minutes past start time",
                        orderId, minutesSinceStart);

                // 1. Tạo fee (30% estimated cost)
                Fee noShowFee = feeCalculationService.calculateNoShowFee(order);
                log.info("Created NO_SHOW fee: {} VNĐ for order {}", noShowFee.getAmount(), orderId);

                // 2. Cập nhật order status
                order.setStatus(Order.Status.CANCELED);
                order.setCanceledAt(now);
                order.setCancellationReason("Không đến theo lịch (No-show)");
                orderRepository.save(order);

                // 3. Tăng violation count
                Long userId = order.getUser().getUserId();
                incrementViolationCount(userId, "Không đến theo lịch đặt");

                // 4. Kiểm tra auto-lock
                boolean isLocked = checkAndAutoLockUser(userId);

                // 5. Gửi email và notification
                String additionalInfo = isLocked ? " Tài khoản bị khóa do vi phạm 3 lần." : "";
                notificationService.createPenaltyNotification(
                        orderId,
                        NotificationServiceImpl.PenaltyEvent.NO_SHOW_PENALTY,
                        noShowFee.getAmount(),
                        "Không đến sau 30 phút." + additionalInfo
                );

                // TODO: Gửi email qua EmailService

                return noShowFee;
            }

            return null;

        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in handleNoShow: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi hệ thống khi xử lý no-show: " + e.getMessage());
        }
    }

    /**
     * @deprecated Overtime charging replaced by parking fee system
     * Use parking fees calculated in endSession() instead
     */
    @Deprecated
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Fee handleOvertimeCharging(Long sessionId, int extraMinutes) {
        log.warn("DEPRECATED: handleOvertimeCharging is no longer used. Parking fees are handled in endSession()");
        throw new UnsupportedOperationException("Overtime charging is deprecated. Use parking fee system instead.");
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateTotalPaymentAmount(Long sessionId) {
        log.info("AC4: Calculating total payment amount for session {}", sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiRequestException("Session không tồn tại"));

        // Lấy base cost
        Double baseCost = session.getBaseCost();

        // Lấy TẤT CẢ fees CHƯA THANH TOÁN
        // ✅ CHỈ lấy fees có isPaid = false
        List<Fee> unpaidFees = feeRepository.findBySessionSessionId(sessionId).stream()
                .filter(fee -> fee.getIsPaid() != null && !fee.getIsPaid())
                .toList();

        // Tính tổng unpaid fees
        Double totalUnpaidFees = unpaidFees.stream()
                .mapToDouble(Fee::getAmount)
                .sum();

        Double totalAmount = baseCost + totalUnpaidFees;

        log.info("Session {} - Base cost: {}, Unpaid fees: {} (count: {}), Total amount: {}",
                sessionId, baseCost, totalUnpaidFees, unpaidFees.size(), totalAmount);

        return totalAmount;
    }

    @Override
    @Transactional
    public boolean checkAndAutoLockUser(Long userId) {
        log.info("AC5: Checking auto-lock for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        if (user.getViolations() >= AUTO_LOCK_VIOLATIONS) {
            log.warn("User {} has {} violations, auto-banning account (temporary)", userId, user.getViolations());

            user.setStatus(User.UserStatus.BANNED); // Tạm khóa, có thể mở bằng thanh toán
            user.setReasonReport(String.format(
                    "Tự động khóa do vi phạm %d lần (ngày %s). " +
                    "Thanh toán phí phạt để mở khóa tài khoản.",
                    user.getViolations(),
                    LocalDateTime.now()
            ));
            userRepository.save(user);

            // Gửi thông báo
            notificationService.createGeneralNotification(
                    List.of(userId),
                    "Tài khoản bị khóa",
                    String.format("Tài khoản của bạn đã bị khóa do vi phạm %d lần. " +
                            "Vui lòng thanh toán phí phạt để mở khóa và tiếp tục sử dụng dịch vụ.", AUTO_LOCK_VIOLATIONS)
            );

            return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeDetailDTO> getUserFeeHistory(Long userId) {
        log.info("AC6: Getting fee history for user {}", userId);

        // Lấy tất cả fees của user
        List<Fee> allFees = getUserFees(userId);

        // Convert to DTO
        return allFees.stream()
                .distinct()
                .map(FeeDetailDTO::fromEntity)
                .sorted((f1, f2) -> f2.getCreatedAt().compareTo(f1.getCreatedAt())) // Newest first
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeDetailDTO> getSessionFeeDetails(Long sessionId) {
        log.info("AC6: Getting fee details for session {}", sessionId);

        List<Fee> fees = feeRepository.findBySessionSessionId(sessionId);
        return fees.stream()
                .map(FeeDetailDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void incrementViolationCount(Long userId, String reason) {
        log.info("Incrementing violation count for user {}: {}", userId, reason);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        user.setViolations(user.getViolations() + 1);

        // Append reason to reasonReport
        String currentReport = user.getReasonReport() != null ? user.getReasonReport() : "";
        String newReport = String.format("%s\n[%s] Vi phạm #%d: %s",
                currentReport,
                LocalDateTime.now(),
                user.getViolations(),
                reason);
        user.setReasonReport(newReport);

        userRepository.save(user);

        log.info("User {} now has {} violations", userId, user.getViolations());
    }


    @Override
    @Transactional(readOnly = true)
    public boolean hasUnpaidFees(Long userId) {
        // Đổi logic: CHỈ check transactions FAILED/PENDING, KHÔNG check fees
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        List<Transaction> unpaidTransactions = transactionRepository
                .findByUserOrderByTransactionIdDesc(user)
                .stream()
                .filter(t -> t.getStatus() == Transaction.Status.FAILED ||
                             t.getStatus() == Transaction.Status.PENDING)
                .filter(t -> t.getSession() != null)
                .collect(Collectors.toList());

        return !unpaidTransactions.isEmpty();
    }

    /**
     * Lấy danh sách và tổng số tiền của tất cả transactions chưa thanh toán (FAILED/PENDING)
     * Method này dùng cho FE để hiển thị tổng hợp trước khi thanh toán
     */
    @Transactional(readOnly = true)
    public UnpaidTransactionsSummary getUnpaidTransactionsSummary(Long userId) {
        log.info("Getting unpaid transactions summary for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        // Lấy tất cả transactions FAILED và PENDING
        List<Transaction> unpaidTransactions = transactionRepository
                .findByUserOrderByTransactionIdDesc(user)
                .stream()
                .filter(t -> t.getStatus() == Transaction.Status.FAILED ||
                             t.getStatus() == Transaction.Status.PENDING)
                .filter(t -> t.getSession() != null)
                .collect(Collectors.toList());

        if (unpaidTransactions.isEmpty()) {
            return new UnpaidTransactionsSummary(0.0, List.of(), List.of());
        }

        // Tính tổng số tiền
        double totalAmount = unpaidTransactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Lấy danh sách transaction IDs
        List<Long> transactionIds = unpaidTransactions.stream()
                .map(Transaction::getTransactionId)
                .collect(Collectors.toList());

        // Tạo summary details
        List<TransactionDetail> details = unpaidTransactions.stream()
                .map(t -> new TransactionDetail(
                    t.getTransactionId(),
                    t.getAmount(),
                    t.getStatus().toString(),
                    t.getSession() != null ? t.getSession().getSessionId() : null,
                    t.getCreatedAt()
                ))
                .collect(Collectors.toList());

        log.info("User {} has {} unpaid transactions, total: {} VNĐ",
                userId, unpaidTransactions.size(), totalAmount);

        return new UnpaidTransactionsSummary(totalAmount, transactionIds, details);
    }

    // Inner classes for response
    public static class UnpaidTransactionsSummary {
        public final double totalAmount;
        public final List<Long> transactionIds;
        public final List<TransactionDetail> details;

        public UnpaidTransactionsSummary(double totalAmount, List<Long> transactionIds,
                                        List<TransactionDetail> details) {
            this.totalAmount = totalAmount;
            this.transactionIds = transactionIds;
            this.details = details;
        }
    }

    public static class TransactionDetail {
        public final Long transactionId;
        public final double amount;
        public final String status;
        public final Long sessionId;
        public final LocalDateTime createdAt;

        public TransactionDetail(Long transactionId, double amount, String status,
                                Long sessionId, LocalDateTime createdAt) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.status = status;
            this.sessionId = sessionId;
            this.createdAt = createdAt;
        }
    }

    @Override
    @Transactional
    public boolean unlockUserAfterPayment(Long userId) {
        log.info("Attempting to unlock user {} after payment", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        // Kiểm tra điều kiện mở khóa
        if (user.getStatus() != User.UserStatus.BANNED) {
            log.warn("User {} is not BANNED (status: {}), cannot unlock", userId, user.getStatus());
            return false;
        }

        // CHỈ check transactions FAILED
        if (hasUnpaidFees(userId)) {
            log.warn("User {} still has failed transactions, cannot unlock", userId);
            return false;
        }

        // Mở khóa: BANNED → ACTIVE
        user.setStatus(User.UserStatus.ACTIVE);
        user.setReasonReport(user.getReasonReport() +
                "\n[Mở khóa: " + LocalDateTime.now() +
                "] Tài khoản được mở khóa sau khi thanh toán tất cả giao dịch thất bại.");
        userRepository.save(user);

        log.info("Successfully unlocked user account: {}", userId);

        // Gửi thông báo
        notificationService.createGeneralNotification(
                List.of(userId),
                "Tài khoản đã được mở khóa",
                "Tài khoản của bạn đã được mở khóa sau khi thanh toán tất cả giao dịch thất bại. " +
                "Bạn có thể tiếp tục sử dụng dịch vụ."
        );

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUnlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User không tồn tại"));

        // Chỉ có thể mở khóa nếu: BANNED và không còn transactions FAILED
        return user.getStatus() == User.UserStatus.BANNED &&
               !hasUnpaidFees(userId);
    }

    // Helper methods
    private List<Fee> getUserFees(Long userId) {
        return feeRepository.findAll().stream()
                .filter(fee ->
                    (fee.getOrder() != null && fee.getOrder().getUser().getUserId().equals(userId)) ||
                    (fee.getSession() != null && fee.getSession().getOrder() != null &&
                     fee.getSession().getOrder().getUser().getUserId().equals(userId))
                )
                .toList();
    }
}

