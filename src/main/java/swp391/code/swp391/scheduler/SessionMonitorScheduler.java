package swp391.code.swp391.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.*;
import swp391.code.swp391.service.NotificationService;
import swp391.code.swp391.service.NotificationServiceImpl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job để tự động hoàn thành sessions khi đạt target battery
 * Tránh trường hợp FE không poll monitorSession() hoặc user offline
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionMonitorScheduler {

    private final SessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Chạy mỗi 2 phút để check sessions đang CHARGING
     * Nếu đã đạt target battery → tự động chuyển PARKING
     */
    @Scheduled(fixedRate = 120000) // 2 phút = 120,000ms
    @Transactional
    public void autoCompleteTargetReachedSessions() {
        try {
            List<Session> chargingSessions = sessionRepository.findByStatus(Session.SessionStatus.CHARGING);

            if (chargingSessions.isEmpty()) {
                return;
            }

            log.info("Checking {} charging sessions for auto-completion", chargingSessions.size());

            int completedCount = 0;

            for (Session session : chargingSessions) {
                try {
                    if (shouldTransitionToParking(session)) {
                        transitionToParking(session);
                        completedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-complete session {}: {}",
                              session.getSessionId(), e.getMessage(), e);
                }
            }

            if (completedCount > 0) {
                log.info("Auto-completed {} sessions to PARKING", completedCount);
            }

        } catch (Exception e) {
            log.error("Error in autoCompleteTargetReachedSessions: {}", e.getMessage(), e);
        }
    }

    /**
     * Check xem session có nên chuyển sang PARKING không
     */
    private boolean shouldTransitionToParking(Session session) {
        Order order = session.getOrder();
        if (order == null) {
            log.warn("Session {} has no order", session.getSessionId());
            return false;
        }

        Vehicle vehicle = order.getVehicle();
        if (vehicle == null || vehicle.getCarModel() == null) {
            log.warn("Session {} has no vehicle/car model", session.getSessionId());
            return false;
        }

        // Tính current battery
        double powerConsumed = session.getPowerConsumed() != null ? session.getPowerConsumed() : 0.0;
        double batteryCapacity = vehicle.getCarModel().getCapacity();
        double currentBattery = (powerConsumed / batteryCapacity * 100.0) + order.getStartedBattery();

        if (currentBattery > 100) {
            currentBattery = 100.0;
        }

        double targetBattery = order.getExpectedBattery();

        log.debug("Session {} - current: {}%, target: {}%",
                  session.getSessionId(),
                  String.format("%.2f", currentBattery),
                  String.format("%.2f", targetBattery));

        return currentBattery >= targetBattery;
    }

    /**
     * Chuyển session sang PARKING
     */
    private void transitionToParking(Session session) {
        LocalDateTime now = LocalDateTime.now();
        Order order = session.getOrder();

        log.info("AUTO-TRANSITIONING Session {} to PARKING (scheduled job)", session.getSessionId());

        // Update session
        session.setEndTime(now);
        session.setStatus(Session.SessionStatus.PARKING);
        session.setParkingStartTime(now);

        // Tính lại power consumed và cost nếu chưa có
        if (session.getPowerConsumed() == null || session.getBaseCost() == null) {
            ConnectorType connectorType = order.getChargingPoint().getConnectorType();
            double power = connectorType.getPowerOutput();
            long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), now);
            double powerConsumed = power * (secondsElapsed / 3600.0);
            double basePrice = connectorType.getPricePerKWh();
            double cost = powerConsumed * basePrice;

            session.setPowerConsumed(powerConsumed);
            session.setBaseCost(cost);
        }

        // Update order
        order.setStatus(Order.Status.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} auto-saved with status: {}", savedOrder.getOrderId(), savedOrder.getStatus());

        // Send notification nếu chưa gửi
        if (!session.getTargetReachedNotificationSent()) {
            try {
                notificationService.createBookingOrderNotification(
                    order.getOrderId(),
                    NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE,
                    "Đã sạc đến mức mong muốn. Vui lòng xác nhận rời trạm trong vòng 15 phút để tránh phí đỗ xe"
                );
                session.setTargetReachedNotificationSent(true);
            } catch (Exception e) {
                log.error("Failed to send notification for session {}: {}",
                          session.getSessionId(), e.getMessage());
            }
        }

        // Save session
        Session savedSession = sessionRepository.save(session);

        log.info("Session {} auto-saved with status: {} (parking started at: {})",
                 savedSession.getSessionId(),
                 savedSession.getStatus(),
                 savedSession.getParkingStartTime());

        // Verify
        Order verifiedOrder = orderRepository.findById(order.getOrderId()).orElse(null);
        if (verifiedOrder != null && verifiedOrder.getStatus() != Order.Status.COMPLETED) {
            log.error("CRITICAL: Auto-completion failed - Order {} status is: {}",
                      order.getOrderId(), verifiedOrder.getStatus());
        } else {
            log.info("✓ Verified: Order {} is COMPLETED in DB", order.getOrderId());
        }
    }

    /**
     * Cleanup sessions bị stuck quá lâu (> 12 giờ) vẫn đang CHARGING
     * Có thể do FE crash, network issue, etc.
     */
    @Scheduled(cron = "0 0 */6 * * *") // Chạy mỗi 6 giờ
    @Transactional
    public void cleanupStuckSessions() {
        try {
            LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);

            List<Session> stuckSessions = sessionRepository.findByStatus(Session.SessionStatus.CHARGING)
                .stream()
                .filter(s -> s.getStartTime().isBefore(twelveHoursAgo))
                .toList();

            if (stuckSessions.isEmpty()) {
                return;
            }

            log.warn("Found {} stuck sessions (>12 hours in CHARGING)", stuckSessions.size());

            for (Session session : stuckSessions) {
                try {
                    log.warn("Force-completing stuck session {} (started at: {})",
                             session.getSessionId(), session.getStartTime());

                    // Force transition regardless of battery level
                    transitionToParking(session);

                } catch (Exception e) {
                    log.error("Failed to cleanup stuck session {}: {}",
                              session.getSessionId(), e.getMessage(), e);
                }
            }

            log.info("Cleaned up {} stuck sessions", stuckSessions.size());

        } catch (Exception e) {
            log.error("Error in cleanupStuckSessions: {}", e.getMessage(), e);
        }
    }
}

