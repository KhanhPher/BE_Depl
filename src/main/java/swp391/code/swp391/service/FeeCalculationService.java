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
}