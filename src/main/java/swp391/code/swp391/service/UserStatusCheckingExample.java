package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.OrderRequestDTO;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.exception.UserStatusException;
import swp391.code.swp391.repository.UserRepository;
import swp391.code.swp391.util.UserStatusChecker;

/**
 * EXAMPLE: How to implement User Status Checking in Services
 *
 * This is a reference implementation showing different ways to check user status
 * in your service layer methods.
 *
 * Choose the approach that best fits your use case.
 */
@Service
@RequiredArgsConstructor
public class UserStatusCheckingExample {

    private final UserRepository userRepository;
    private final UserStatusChecker userStatusChecker;

    // ==================== APPROACH 1: Using UserStatusChecker ====================

    /**
     * RECOMMENDED: Use UserStatusChecker utility
     *
     * Best for: Methods where you already have authenticated user context
     * Pros: Clean, simple, throws proper exception automatically
     * Cons: Requires JWT authentication context
     */
    public void createOrder_Approach1(OrderRequestDTO request) {
        // Check status of current authenticated user from JWT
        userStatusChecker.requireActiveStatus();

        // If reaches here, user is ACTIVE - proceed with logic
        // ... your order creation logic ...
    }

    // ==================== APPROACH 2: Manual Check with User Object ====================

    /**
     * Use when you have User object from database
     *
     * Best for: Methods that already load user from repository
     * Pros: Works even without JWT context, can provide custom messages
     * Cons: Requires fetching user from DB
     */
    public void updateProfile_Approach2(Long userId, String newName) {
        // Load user from database
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Check status using utility
        userStatusChecker.requireActiveStatus(user);

        // Or manual check with custom message:
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UserStatusException(
                user.getStatus(),
                "Không thể cập nhật profile. Tài khoản của bạn " +
                (user.getStatus() == User.UserStatus.BANNED ? "đã bị khóa" : "chưa được kích hoạt"),
                user.getStatus() == User.UserStatus.BANNED ? "USER_BANNED" : "USER_INACTIVE"
            );
        }

        // Proceed with update
        user.setFullName(newName);
        userRepository.save(user);
    }

    // ==================== APPROACH 3: Check Before Complex Operation ====================

    /**
     * Check status before expensive operations
     *
     * Best for: Long-running operations, database-intensive operations
     * Pros: Fail fast, save resources
     */
    public void startChargingSession_Approach3(Long userId, Long orderId) {
        // Check status FIRST before any expensive operations
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            String message = user.getStatus() == User.UserStatus.BANNED
                ? "Tài khoản bị khóa. Không thể bắt đầu phiên sạc. Lý do: " + user.getReasonReport()
                : "Tài khoản chưa kích hoạt. Vui lòng kích hoạt tài khoản trước.";

            throw new UserStatusException(user.getStatus(), message);
        }

        // Now proceed with expensive operations
        // ... load order, charging point, validate, start session ...
    }

    // ==================== APPROACH 4: Conditional Check ====================

    /**
     * Conditional checking for different operations
     *
     * Best for: Methods with multiple operation types
     */
    public void handleVehicleOperation_Approach4(Long userId, String operation, Long vehicleId) {
        // View operations allowed for all
        if ("VIEW".equals(operation)) {
            // Anyone can view (already handled by filter)
            // ... return vehicle info ...
            return;
        }

        // Modification operations require ACTIVE status
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if ("ADD".equals(operation) || "UPDATE".equals(operation) || "DELETE".equals(operation)) {
            userStatusChecker.requireActiveStatus(user);

            // Proceed with modification
            // ... add/update/delete vehicle ...
        }
    }

    // ==================== APPROACH 5: Return User-Friendly Message ====================

    /**
     * Provide detailed feedback to user
     *
     * Best for: User-facing operations where clear feedback is important
     */
    public void subscribeToPackage_Approach5(Long userId, Long subscriptionId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        switch (user.getStatus()) {
            case BANNED:
                throw new UserStatusException(
                    User.UserStatus.BANNED,
                    "Không thể đăng ký gói. Tài khoản của bạn đã bị khóa vì: " +
                    (user.getReasonReport() != null ? user.getReasonReport() : "Vi phạm chính sách"),
                    "USER_BANNED"
                );

            case INACTIVE:
                throw new UserStatusException(
                    User.UserStatus.INACTIVE,
                    "Vui lòng kích hoạt tài khoản trước khi đăng ký gói. " +
                    "Kiểm tra email để nhận link kích hoạt.",
                    "USER_INACTIVE"
                );

            case ACTIVE:
                // Proceed with subscription
                // ... subscribe user to package ...
                break;
        }
    }

    // ==================== APPROACH 6: Checking in Helper Method ====================

    /**
     * Centralize status check in helper method
     *
     * Best for: When you have multiple methods needing same check
     */
    private void validateUserCanPerformTransaction(User user) {
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            String reason = user.getStatus() == User.UserStatus.BANNED
                ? "USER_BANNED" : "USER_INACTIVE";
            String message = user.getStatus() == User.UserStatus.BANNED
                ? "Tài khoản bị khóa. Liên hệ admin@chargehub.com để được hỗ trợ."
                : "Tài khoản chưa kích hoạt. Vui lòng kích hoạt để tiếp tục.";

            throw new UserStatusException(user.getStatus(), message, reason);
        }
    }

    public void makePayment(Long userId, Double amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Use helper method
        validateUserCanPerformTransaction(user);

        // Proceed with payment
        // ... payment logic ...
    }

    public void createIssueReport(Long userId, String description) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Use same helper method
        validateUserCanPerformTransaction(user);

        // Proceed with creating report
        // ... create issue report ...
    }

    // ==================== APPROACH 7: Using UserStatusChecker Boolean Methods ====================

    /**
     * Use boolean check methods for conditional logic
     *
     * Best for: Feature flags, optional features based on status
     */
    public void sendNotification_Approach7(Long userId, String message) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is active using boolean methods
        if (!isUserActive(user)) {
            // Log but don't fail
            System.out.println("User " + userId + " is not active. Notification not sent.");
            return;
        }

        // Send notification
        // ... send notification logic ...
    }

    private boolean isUserActive(User user) {
        return user.getStatus() == User.UserStatus.ACTIVE;
    }

    // ==================== APPROACH 8: Status-Specific Handling ====================

    /**
     * Different handling for different status
     *
     * Best for: When you need status-specific business logic
     */
    public void attemptAutoRenewal_Approach8(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        switch (user.getStatus()) {
            case ACTIVE:
                // Proceed with auto-renewal
                // ... renew subscription ...
                break;

            case INACTIVE:
                // Don't renew, send activation reminder
                // ... send reminder email ...
                break;

            case BANNED:
                // Cancel auto-renewal completely
                user.setSubscriptionAutoRenew(false);
                userRepository.save(user);
                break;
        }
    }

    // ==================== NOTES AND BEST PRACTICES ====================

    /**
     * WHEN TO USE EACH APPROACH:
     *
     * 1. UserStatusChecker.requireActiveStatus()
     *    - Quick check in authenticated context
     *    - Most common use case
     *    - Clean and simple
     *
     * 2. Manual check with User object
     *    - When you need custom error messages
     *    - When you already have user from DB
     *    - When you need to access user.reasonReport
     *
     * 3. Check before expensive operations
     *    - Database-heavy operations
     *    - External API calls
     *    - Long-running processes
     *
     * 4. Conditional checks
     *    - Multiple operation types
     *    - Different rules for different actions
     *
     * 5. Detailed feedback
     *    - User-facing features
     *    - Where UX is important
     *    - Need to explain WHY action is blocked
     *
     * 6. Helper methods
     *    - Multiple methods need same check
     *    - Consistent validation across service
     *
     * 7. Boolean checks
     *    - Optional features
     *    - Logging/monitoring
     *    - Soft failures acceptable
     *
     * 8. Status-specific logic
     *    - Different behavior per status
     *    - Business rules vary by status
     *
     * IMPORTANT NOTES:
     *
     * - Filter already blocks main flow endpoints for BANNED/INACTIVE users
     * - Service layer checks provide additional safety and custom messages
     * - For most cases, filter is enough - service checks are optional
     * - Use service checks when you need:
     *   * Custom error messages
     *   * Access to user.reasonReport
     *   * Status-specific business logic
     *   * Double-check critical operations
     *
     * - Remember: Status in JWT might be outdated if changed in DB
     *   User needs to re-login for JWT to reflect new status
     */
}

