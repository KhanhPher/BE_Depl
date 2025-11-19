package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.entity.Fee;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.repository.FeeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationServiceImpl implements FeeCalculationService {

    private final FeeRepository feeRepository;

    // Các hằng số cấu hình
    private static final BigDecimal OVERCHARGE_RATE = new BigDecimal("2000.00"); // VND mỗi phút
    private static final BigDecimal NO_SHOW_RATE = new BigDecimal("0.30"); // 30%
    private static final BigDecimal CANCEL_RATE = new BigDecimal("0.10"); // 10%

    @Override
    @Transactional
    public Fee calculateChargingFee(Session session, int extraMinutes) {
        log.info("Đang tính phí CHARGING cho session: {}, số phút thêm: {}",
                session.getSessionId(), extraMinutes);

        if (extraMinutes <= 0) {
            return null;
        }

        BigDecimal amount = OVERCHARGE_RATE.multiply(new BigDecimal(extraMinutes));

        Fee fee = new Fee();
        fee.setSession(session);
        fee.setType(Fee.Type.OVERTIME); // AC3: OVERTIME type
        fee.setAmount(amount.doubleValue()); // Convert to Double for existing entity
        fee.setDescription(String.format(
                "Phí sạc quá giờ: %d phút × %s VNĐ/phút",
                extraMinutes, OVERCHARGE_RATE.toString()
        ));
        fee.setIsPaid(false);
        fee.setCreatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    @Transactional
    public Fee calculateNoShowFee(Order order) {
        log.info("Đang tính phí NO_SHOW cho order: {}", order.getOrderId());

        // Ước tính chi phí đơn đặt dựa trên công suất dự kiến
        BigDecimal estimatedCost = estimateOrderCost(order);
        BigDecimal amount = estimatedCost.multiply(NO_SHOW_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        Fee fee = new Fee();
        fee.setOrder(order);
        fee.setType(Fee.Type.NO_SHOW);
        fee.setAmount(amount.doubleValue());
        fee.setDescription(String.format(
                "Phí không đến theo lịch: %s%% × %s VNĐ ước tính",
                NO_SHOW_RATE.multiply(new BigDecimal("100")).toString(),
                estimatedCost.toString()
        ));
        fee.setIsPaid(false);
        fee.setCreatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    @Transactional
    public Fee calculateCancelFee(Order order) {
        log.info("Đang tính phí CANCEL cho order: {}", order.getOrderId());

        // Ước tính chi phí đơn đặt
        BigDecimal estimatedCost = estimateOrderCost(order);
        BigDecimal amount = estimatedCost.multiply(CANCEL_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        Fee fee = new Fee();
        fee.setOrder(order);
        fee.setType(Fee.Type.CANCEL);
        fee.setAmount(amount.doubleValue());
        fee.setDescription(String.format(
                "Phí hủy lịch muộn (< 10 phút): %s%% × %s VNĐ ước tính",
                CANCEL_RATE.multiply(new BigDecimal("100")).toString(),
                estimatedCost.toString()
        ));
        fee.setIsPaid(false);
        fee.setCreatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Override
    public List<Fee> getSessionFees(Long sessionId) {
        return feeRepository.findBySessionSessionId(sessionId);
    }

    @Override
    public List<Fee> getUnpaidSessionFees(Long sessionId) {
        log.info("Lấy các khoản phí CHƯA THANH TOÁN cho session: {}", sessionId);
        return feeRepository.findBySessionSessionId(sessionId).stream()
                .filter(fee -> fee.getIsPaid() != null && !fee.getIsPaid())
                .toList();
    }

    @Override
    public BigDecimal calculateTotalFees(List<Fee> fees) {
        if (fees == null || fees.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return fees.stream()
                .map(fee -> BigDecimal.valueOf(fee.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Ước tính chi phí đơn đặt dựa trên dung lượng pin xe và giá connector type
     */
    private BigDecimal estimateOrderCost(Order order) {
        // Ước tính cơ bản: giả sử sạc 50% dung lượng pin
        // Trong thực tế, sử dụng order.expectedPower hoặc dung lượng xe

        BigDecimal estimatedPower = new BigDecimal("30.00"); // Mặc định 30 kWh

        if (order.getVehicle() != null && order.getVehicle().getCarModel() != null) {
            // Giả sử sạc 50% dung lượng pin
            estimatedPower = BigDecimal.valueOf(order.getVehicle().getCarModel().getCapacity())
                    .multiply(new BigDecimal("0.5"));
        }

        // Lấy giá cơ bản từ connector type
        BigDecimal basePrice = new BigDecimal("3500.00"); // Giá mặc định
        if (order.getChargingPoint() != null &&
                order.getChargingPoint().getConnectorType() != null) {
            basePrice = BigDecimal.valueOf(
                    order.getChargingPoint().getConnectorType().getPricePerKWh()
            );
        }

        // Ước tính đơn giản: power × basePrice × hệ số giá trung bình (1.2)
        return estimatedPower.multiply(basePrice)
                .multiply(new BigDecimal("1.2"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}