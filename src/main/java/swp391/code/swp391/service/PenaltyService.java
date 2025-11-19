package swp391.code.swp391.service;

import swp391.code.swp391.dto.FeeDetailDTO;
import swp391.code.swp391.entity.Fee;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.Session;

import java.util.List;

/**
 * Service xử lý các loại phí phạt theo Business Rules
 *
 * AC1: Hủy < 10 phút trước startTime → Fee CANCEL (10%), violations +1
 * AC2: Không đến sau 30 phút → Order CANCELED, Fee NO_SHOW (30%), violations +1
 * AC3: Pin đầy nhưng vẫn kết nối > 1 phút → Fee OVERTIME (2,000 VNĐ/phút)
 * AC4: Thanh toán Session → Transaction.amount = Session.cost + sum(Fee.amount)
 * AC5: violationCount = 3 → Auto BANNED
 * AC6: Xem lịch sử fee chi tiết
 * AC7: Config phí có thể thay đổi (không ảnh hưởng phí cũ)
 * AC8: Rollback nếu lỗi
 */
public interface PenaltyService {

    /**
     * AC1: Xử lý hủy order muộn (< 10 phút trước startTime)
     * @param orderId Order ID
     * @param userId User ID thực hiện hủy
     * @param reason Lý do hủy
     * @return Fee được tạo (10% estimatedCost)
     * @throws Exception nếu không thể hủy hoặc lỗi hệ thống
     */
    Fee handleLateCancellation(Long orderId, Long userId, String reason);

    /**
     * AC2: Xử lý không đến sau 30 phút - No-show
     * Được gọi bởi Scheduler
     * @param orderId Order ID
     * @return Fee được tạo (30% estimatedCost)
     */
    Fee handleNoShow(Long orderId);

    /**
     * AC3: Xử lý overtime khi pin đầy nhưng vẫn kết nối
     * Được gọi real-time khi detect battery full
     * @param sessionId Session ID
     * @param extraMinutes Số phút quá giờ
     * @return Fee được tạo (2,000 VNĐ/phút)
     */
    Fee handleOvertimeCharging(Long sessionId, int extraMinutes);

    /**
     * AC4: Tính tổng số tiền thanh toán bao gồm phí phạt
     * @param sessionId Session ID
     * @return Tổng số tiền (baseCost + sum of fees)
     */
    Double calculateTotalPaymentAmount(Long sessionId);

    /**
     * AC5: Kiểm tra và tự động khóa tài khoản nếu đủ 3 vi phạm
     * @param userId User ID
     * @return true nếu bị khóa, false nếu chưa
     */
    boolean checkAndAutoLockUser(Long userId);

    /**
     * AC6: Lấy chi tiết tất cả các phí của user
     * @param userId User ID
     * @return Danh sách fee details
     */
    List<FeeDetailDTO> getUserFeeHistory(Long userId);

    /**
     * AC6: Lấy chi tiết phí của một session
     * @param sessionId Session ID
     * @return Danh sách fee details
     */
    List<FeeDetailDTO> getSessionFeeDetails(Long sessionId);

    /**
     * Tăng violation count cho user
     * @param userId User ID
     * @param reason Lý do vi phạm
     */
    void incrementViolationCount(Long userId, String reason);

    /**
     * Kiểm tra user có transactions FAILED/PENDING không
     * @param userId User ID
     * @return true nếu có transactions FAILED hoặc PENDING
     */
    boolean hasUnpaidFees(Long userId);

    /**
     * Lấy tổng hợp tất cả transactions chưa thanh toán (FAILED/PENDING)
     * Dùng cho FE hiển thị tổng số tiền trước khi thanh toán
     * @param userId User ID
     * @return UnpaidTransactionsSummary chứa totalAmount, transactionIds, và details
     */
    PenaltyServiceImpl.UnpaidTransactionsSummary getUnpaidTransactionsSummary(Long userId);

    /**
     * Mở khóa tài khoản user sau khi thanh toán hết transactions thất bại
     * Chuyển status từ BANNED → ACTIVE
     * @param userId User ID
     * @return true nếu mở khóa thành công
     */
    boolean unlockUserAfterPayment(Long userId);

    /**
     * Kiểm tra user có thể mở khóa không
     * (status = BANNED và không còn transactions FAILED)
     * @param userId User ID
     * @return true nếu có thể mở khóa
     */
    boolean canUnlockUser(Long userId);
}
