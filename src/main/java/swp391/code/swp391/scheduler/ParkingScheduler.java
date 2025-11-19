package swp391.code.swp391.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.repository.SessionRepository;
import swp391.code.swp391.service.EmailService;

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

    // Track sessions đã gửi email nhắc nhở 1 giờ (tránh gửi trùng)
    private final Set<Long> warnedSessionIds = new HashSet<>();

    /**
     * Mỗi 5 phút: kiểm tra các session STILL_PARKED quá 1 giờ kể từ khi kết thúc phiên sạc.
     * Điều kiện gửi mail:
     *  - Session.status == STILL_PARKED
     *  - now >= endTime + 60 phút (ưu tiên theo yêu cầu nghiệp vụ)
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
                if (s.getStatus() != Session.SessionStatus.STILL_PARKED) continue;
                if (s.getEndTime() == null) continue;
                if (warnedSessionIds.contains(s.getSessionId())) continue; // tránh gửi trùng

                totalChecked++;

                LocalDateTime threshold = s.getEndTime().plusHours(1);
                if (now.isBefore(threshold)) continue; // chưa đủ 1 giờ

                try {
                    // Chuẩn bị dữ liệu email
                    var order = s.getOrder();
                    var user = order.getUser();
                    String toEmail = user.getEmail();
                    String userName = user.getFullName();
                    String stationName = order.getChargingPoint().getStation().getStationName();
                    String chargingPointName = order.getChargingPoint().getChargingPointName();

                    long parkedMinutes = Math.max(0, Duration.between(s.getEndTime(), now).toMinutes());
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    String endTimeStr = s.getEndTime().format(fmt);

                    emailService.sendStillParkedAfterOneHourEmail(
                            toEmail,
                            userName,
                            stationName,
                            chargingPointName,
                            endTimeStr,
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

            // Cleanup đã gửi cho những session không còn STILL_PARKED
            cleanupWarnedSessionIds();

        } catch (Exception e) {
            log.error("Error in ParkingScheduler: {}", e.getMessage(), e);
        }
    }

    private void cleanupWarnedSessionIds() {
        try {
            Set<Long> activeStillParkedIds = sessionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == Session.SessionStatus.STILL_PARKED)
                    .map(Session::getSessionId)
                    .collect(java.util.stream.Collectors.toSet());
            warnedSessionIds.retainAll(activeStillParkedIds);
        } catch (Exception e) {
            log.error("Error cleaning warned session IDs: {}", e.getMessage(), e);
        }
    }
}

