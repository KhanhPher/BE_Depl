package swp391.code.swp391.service;

import swp391.code.swp391.entity.Fee;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.Session;

import java.math.BigDecimal;
import java.util.List;

public interface FeeCalculationService {

    /**
     * Tính phí CHARGING (phí sạc quá thời gian khi pin đã đầy)
     * Công thức: overchargeRate × extraMinutes
     */
    Fee calculateChargingFee(Session session, int extraMinutes);

    /**
     * Tính phí NO_SHOW (phí không đến theo lịch đặt)
     * Công thức: 30% của chi phí ước tính trong đơn đặt
     */
    Fee calculateNoShowFee(Order order);

    /**
     * Tính phí CANCEL (phí hủy lịch muộn < 10 phút)
     * Công thức: 10% của chi phí ước tính trong đơn đặt
     */
    Fee calculateCancelFee(Order order);

    /**
     * Lấy tất cả các khoản phí của một phiên sạc
     */
    List<Fee> getSessionFees(Long sessionId);

    /**
     * Lấy TẤT CẢ các khoản phí CHƯA THANH TOÁN của một phiên sạc
     * CHỈ lấy fees có isPaid = false
     */
    List<Fee> getUnpaidSessionFees(Long sessionId);

    /**
     * Tính tổng số tiền phí
     */
    BigDecimal calculateTotalFees(List<Fee> fees);

    /**
     * Tính phí PARKING (phí đỗ xe sau khi hoàn thành sạc)
     * Công thức: baseRate * minutes * (1 + 0.5 * floor(hours))
     * - baseRate: 500 VND/phút
     * - Mỗi giờ tăng thêm 50% phí
     * - Phí tối thiểu: 10,000 VND
     *
     * @param parkedMinutes Số phút đỗ xe (sau khi trừ 15 phút grace period)
     * @return Phí đỗ xe tính bằng VND
     */
    double calculateParkingFee(long parkedMinutes);

    /**
     * Tạo và lưu parking fee cho session
     *
     * @param session Session đỗ xe
     * @param chargeableMinutes Số phút tính phí (đã trừ grace period)
     * @return Fee entity đã được save, hoặc null nếu không có phí
     */
    Fee createParkingFee(Session session, long chargeableMinutes);
}