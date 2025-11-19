package swp391.code.swp391.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.repository.OrderRepository;
import swp391.code.swp391.service.EmailService;
import swp391.code.swp391.service.PenaltyService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyScheduler {

    private final OrderRepository orderRepository;
    private final PenaltyService penaltyService;
    private final EmailService emailService;

    private static final int NO_SHOW_MINUTES = 30; // 30 phút
    private static final int WARNING_MINUTES = 15; // 15 phút - gửi cảnh báo

    // Set để track các order đã gửi email cảnh báo (tránh gửi trùng)
    private final Set<Long> warnedOrderIds = new HashSet<>();

    @Scheduled(fixedRate = 1000*60*5) //5mins
    public void checkNoShowOrders() {
        log.info("Running no-show checker...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoffTime = now.minusMinutes(NO_SHOW_MINUTES);

            // Tìm các order BOOKED có startTime đã qua 30 phút
            List<Order> potentialNoShows = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus() == Order.Status.BOOKED)
                    .filter(order -> order.getStartTime().isBefore(cutoffTime))
                    .toList();

            log.info("Found {} potential no-show orders", potentialNoShows.size());

            for (Order order : potentialNoShows) {
                try {
                    Duration timeSinceStart = Duration.between(order.getStartTime(), now);
                    long minutesSinceStart = timeSinceStart.toMinutes();

                    log.info("Processing no-show for order {} ({} minutes past start time)",
                            order.getOrderId(), minutesSinceStart);

                    penaltyService.handleNoShow(order.getOrderId());

                } catch (Exception e) {
                    log.error("Error processing no-show for order {}: {}",
                            order.getOrderId(), e.getMessage(), e);
                    // Continue với orders khác
                }
            }

            log.info("No-show checker completed. Processed {} orders", potentialNoShows.size());

        } catch (Exception e) {
            log.error("Error in no-show checker: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi email cảnh báo sau 15 phút nếu user chưa check-in
     * Chạy mỗi 3 phút
     */
    @Scheduled(fixedRate = 1000*60*3) //3 mins
    public void sendNoShowWarningEmails() {
        log.debug("Running no-show warning email sender...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningCutoff = now.minusMinutes(WARNING_MINUTES);

            // Tìm các order BOOKED có startTime đã qua 15 phút nhưng chưa đến 30 phút
            List<Order> ordersNeedingWarning = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus() == Order.Status.BOOKED)
                    .filter(order -> {
                        long minutesSinceStart = Duration.between(order.getStartTime(), now).toMinutes();
                        return minutesSinceStart >= WARNING_MINUTES && minutesSinceStart < NO_SHOW_MINUTES;
                    })
                    .filter(order -> !warnedOrderIds.contains(order.getOrderId())) // Chưa gửi email
                    .toList();

            log.info("Found {} orders needing no-show warning email", ordersNeedingWarning.size());

            for (Order order : ordersNeedingWarning) {
                try {
                    long minutesSinceStart = Duration.between(order.getStartTime(), now).toMinutes();
                    int minutesRemaining = (int) (NO_SHOW_MINUTES - minutesSinceStart);

                    String userName = order.getUser().getFullName();
                    String userEmail = order.getUser().getEmail();
                    String stationName = order.getChargingPoint().getStation().getStationName();
                    String chargingPointName = order.getChargingPoint().getChargingPointName();

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    String startTime = order.getStartTime().format(formatter);

                    log.info("Sending no-show warning email for order {} to {} ({} minutes remaining)",
                            order.getOrderId(), userEmail, minutesRemaining);

                    // Gửi email cảnh báo
                    emailService.sendNoShowWarningEmail(
                            userEmail,
                            userName,
                            order.getOrderId(),
                            stationName,
                            chargingPointName,
                            startTime,
                            minutesRemaining
                    );

                    // Đánh dấu đã gửi
                    warnedOrderIds.add(order.getOrderId());

                    log.info("Successfully sent warning email for order {}", order.getOrderId());

                } catch (Exception e) {
                    log.error("Error sending warning email for order {}: {}",
                            order.getOrderId(), e.getMessage(), e);
                }
            }

            // Cleanup: Xóa các order đã bị cancel/charging khỏi warned set
            cleanupWarnedOrderIds();

        } catch (Exception e) {
            log.error("Error in no-show warning email sender: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup các order IDs đã warned nhưng không còn BOOKED
     */
    private void cleanupWarnedOrderIds() {
        try {
            Set<Long> activeBookedOrderIds = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus() == Order.Status.BOOKED)
                    .map(Order::getOrderId)
                    .collect(java.util.stream.Collectors.toSet());

            warnedOrderIds.retainAll(activeBookedOrderIds);

        } catch (Exception e) {
            log.error("Error cleaning up warned order IDs: {}", e.getMessage());
        }
    }


    @Scheduled(fixedRate = 1000*60*3) //3 mins
    public void sendNoShowReminders() {
        log.debug("Running no-show reminder checker...");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime = now.plusMinutes(10); // 10 phút nữa

            // Tìm orders sắp bị no-show
            List<Order> upcomingOrders = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus() == Order.Status.BOOKED)
                    .filter(order -> order.getStartTime().isBefore(reminderTime) &&
                                   order.getStartTime().isAfter(now))
                    .toList();

            log.debug("Found {} orders needing reminder", upcomingOrders.size());

            for (Order order : upcomingOrders) {
                try {
                    // TODO: Gửi notification/email reminder
                    log.info("Reminder: Order {} starts in 10 minutes", order.getOrderId());

                } catch (Exception e) {
                    log.error("Error sending reminder for order {}: {}",
                            order.getOrderId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in reminder checker: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Cleanup old completed/canceled orders
     * Chạy hàng ngày vào 2:00 AM
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldOrders() {
        log.info("Running order cleanup...");

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // 90 ngày trước

            // TODO: Archive hoặc cleanup old orders
            log.info("Order cleanup completed");

        } catch (Exception e) {
            log.error("Error in order cleanup: {}", e.getMessage(), e);
        }
    }
}

