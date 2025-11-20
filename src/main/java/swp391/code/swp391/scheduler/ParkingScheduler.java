package swp391.code.swp391.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.repository.SessionRepository;
import swp391.code.swp391.service.EmailService;
import swp391.code.swp391.service.NotificationService;
import swp391.code.swp391.service.NotificationServiceImpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingScheduler {

    private final SessionRepository sessionRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // Track sessions đã gửi email nhắc nhở 1 giờ (tránh gửi trùng)
    private final Set<Long> warnedSessionIds = new HashSet<>();

    // Track sessions đã gửi reminder sau 15 phút (tránh gửi trùng)
    private final Set<Long> reminded15MinSessionIds = new HashSet<>();

    /**
     * Mỗi 1 phút: Gửi reminder cho user đang ở trạng thái PARKING quá 15 phút
     * để nhắc họ xác nhận rời đi, nếu không sẽ bắt đầu tính phí từ khi họ confirm
     */
    @Scheduled(fixedRate = 1000 * 60) // Chạy mỗi 1 phút
    public void sendParkingReminderAfter15Minutes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Session> parkingSessions = sessionRepository.findByStatus(Session.SessionStatus.PARKING);

            int remindedCount = 0;

            for (Session session : parkingSessions) {
                if (session.getParkingStartTime() == null) continue;
                if (reminded15MinSessionIds.contains(session.getSessionId())) continue;

                // Kiểm tra đã qua 15 phút chưa
                long minutesSinceParking = Duration.between(session.getParkingStartTime(), now).toMinutes();

                if (minutesSinceParking >= 15) {
                    try {
                        // Gửi notification nhắc nhở
                        notificationService.createBookingOrderNotification(
                            session.getOrder().getOrderId(),
                            NotificationServiceImpl.NotificationEvent.SESSION_STILL_PARKED,
                            "CẢNH BÁO: Đã hết grace period 15 phút! Vui lòng xác nhận rời trạm ngay. " +
                            "Nếu không, phí đỗ xe sẽ được tính khi bạn xác nhận."
                        );

                        reminded15MinSessionIds.add(session.getSessionId());
                        remindedCount++;

                        log.info("Sent 15-min parking reminder for session {} (order {})",
                            session.getSessionId(), session.getOrder().getOrderId());
                    } catch (Exception e) {
                        log.error("Failed to send 15-min reminder for session {}: {}",
                            session.getSessionId(), e.getMessage());
                    }
                }
            }

            if (remindedCount > 0) {
                log.info("Sent {} parking reminders (15-min grace period expired)", remindedCount);
            }

            // Cleanup reminded sessions that are no longer in PARKING status
            cleanupReminded15MinSessions();

        } catch (Exception e) {
            log.error("Error in sendParkingReminderAfter15Minutes: {}", e.getMessage(), e);
        }
    }

    private void cleanupReminded15MinSessions() {
        try {
            Set<Long> activeParkingIds = sessionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == Session.SessionStatus.PARKING)
                    .map(Session::getSessionId)
                    .collect(java.util.stream.Collectors.toSet());
            reminded15MinSessionIds.retainAll(activeParkingIds);
        } catch (Exception e) {
            log.error("Error cleaning reminded 15-min session IDs: {}", e.getMessage(), e);
        }
    }

    /**
     * Mỗi 5 phút: kiểm tra các session PARKING quá 1 giờ kể từ khi bắt đầu parking.
     * Điều kiện gửi mail:
     *  - Session.status == PARKING
     *  - now >= parkingStartTime + 60 phút
     *  - Chưa từng gửi email cho session này (trong vòng đời app)
     */
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void sendStillParkedAfterOneHourEmails() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Session> sessions = sessionRepository.findAll();

            long totalChecked = 0;
            long totalWarned = 0;

            for (Session s : sessions) {
                if (s.getStatus() != Session.SessionStatus.PARKING) continue;
                if (s.getParkingStartTime() == null) continue;
                if (warnedSessionIds.contains(s.getSessionId())) continue; // tránh gửi trùng

                totalChecked++;

                LocalDateTime threshold = s.getParkingStartTime().plusHours(1);
                if (now.isBefore(threshold)) continue; // chưa đủ 1 giờ

                try {
                    // Chuẩn bị dữ liệu email
                    var order = s.getOrder();
                    var user = order.getUser();
                    String toEmail = user.getEmail();
                    String userName = user.getFullName();
                    String stationName = order.getChargingPoint().getStation().getStationName();
                    String chargingPointName = order.getChargingPoint().getChargingPointName();

                    long parkedMinutes = Math.max(0, Duration.between(s.getParkingStartTime(), now).toMinutes());
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    String parkingTimeStr = s.getParkingStartTime().format(fmt);

                    emailService.sendStillParkedAfterOneHourEmail(
                            toEmail,
                            userName,
                            stationName,
                            chargingPointName,
                            parkingTimeStr,
                            parkedMinutes
                    );

                    warnedSessionIds.add(s.getSessionId());
                    totalWarned++;
                    log.info("Sent still-parked 1h email for session {} (order {}, user {})", s.getSessionId(), order.getOrderId(), user.getUserId());

                } catch (Exception e) {
                    log.error("Error sending still-parked email for session {}: {}", s.getSessionId(), e.getMessage(), e);
                }
            }

            if (totalChecked > 0)
                log.info("ParkingScheduler: checked {} sessions, sent {} emails", totalChecked, totalWarned);

            // Cleanup đã gửi cho những session không còn PARKING
            cleanupWarnedSessionIds();

        } catch (Exception e) {
            log.error("Error in ParkingScheduler: {}", e.getMessage(), e);
        }
    }

    private void cleanupWarnedSessionIds() {
        try {
            Set<Long> activeParkingIds = sessionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == Session.SessionStatus.PARKING)
                    .map(Session::getSessionId)
                    .collect(java.util.stream.Collectors.toSet());
            warnedSessionIds.retainAll(activeParkingIds);
        } catch (Exception e) {
            log.error("Error cleaning warned session IDs: {}", e.getMessage(), e);
        }
    }
}

