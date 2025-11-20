package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.ParkingMonitorDTO;
import swp391.code.swp391.entity.ChargingPoint;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.repository.SessionRepository;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for monitoring parking duration in real-time
 * Uses caching to reduce database load during frequent polling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingMonitorService {

    private final SessionRepository sessionRepository;

    // Constants
    private static final long GRACE_PERIOD_MINUTES = 15;
    private static final double PARKING_RATE_PER_MINUTE = 5000.0; // 5,000 VNĐ/phút

    /**
     * Get parking monitoring info for a session
     * Mỗi lần poll sẽ query DB trực tiếp để đảm bảo giá trị chính xác
     *
     * @param sessionId Session ID
     * @param userId User ID (for authorization)
     * @return Parking monitor DTO with real-time info
     */
    public ParkingMonitorDTO getParkingStatus(Long sessionId, Long userId) {
        log.debug("Fetching parking status for session: {} (userId: {})", sessionId, userId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Authorization check
        if (!session.getOrder().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to monitor this session");
        }

        // Only monitor PARKING status
        if (session.getStatus() != Session.SessionStatus.PARKING) {
            throw new RuntimeException("Session is not in PARKING status. Current status: " + session.getStatus());
        }

        if (session.getParkingStartTime() == null) {
            throw new RuntimeException("Parking start time not set");
        }

        return buildParkingMonitorDTO(session);
    }

    /**
     * Build parking monitor DTO with all calculated fields
     */
    private ParkingMonitorDTO buildParkingMonitorDTO(Session session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime parkingStart = session.getParkingStartTime();

        // Calculate durations
        long totalSeconds = Duration.between(parkingStart, now).getSeconds();
        long totalMinutes = totalSeconds / 60;

        // Grace period calculations
        long gracePeriodSeconds = GRACE_PERIOD_MINUTES * 60;
        long remainingGraceSeconds = Math.max(0, gracePeriodSeconds - totalSeconds);
        long remainingGraceMinutes = remainingGraceSeconds / 60;
        boolean isGracePeriodExpired = totalMinutes >= GRACE_PERIOD_MINUTES;

        // Fee calculations
        long chargeableMinutes = Math.max(0, totalMinutes - GRACE_PERIOD_MINUTES);
        double estimatedFee = chargeableMinutes * PARKING_RATE_PER_MINUTE;

        // Warning level and message
        String warningLevel = ParkingMonitorDTO.calculateWarningLevel(totalMinutes, GRACE_PERIOD_MINUTES);
        String warningMessage = ParkingMonitorDTO.generateWarningMessage(
                totalMinutes, remainingGraceMinutes, isGracePeriodExpired);

        // Station info
        Order order = session.getOrder();
        ChargingPoint chargingPoint = order.getChargingPoint();
        String stationName = chargingPoint.getStation().getStationName();
        String chargingPointName = chargingPoint.getChargingPointName();

        // Charging cost từ session (đã được tính khi chuyển sang PARKING)
        Double chargingCost = session.getBaseCost() != null ? session.getBaseCost() : 0.0;
        Double powerConsumed = session.getPowerConsumed() != null ? session.getPowerConsumed() : 0.0;

        return ParkingMonitorDTO.builder()
                .sessionId(session.getSessionId())
                .orderId(order.getOrderId())
                .status(session.getStatus().name())

                // Time tracking
                .parkingStartTime(parkingStart)
                .currentTime(now)
                .totalParkingMinutes(totalMinutes)
                .totalParkingSeconds(totalSeconds)

                // Grace period
                .gracePeriodMinutes(GRACE_PERIOD_MINUTES)
                .remainingGraceMinutes(remainingGraceMinutes)
                .remainingGraceSeconds(remainingGraceSeconds)
                .isGracePeriodExpired(isGracePeriodExpired)

                // Fee preview
                .chargeableMinutes(chargeableMinutes)
                .estimatedParkingFee(estimatedFee)
                .parkingRatePerMinute(PARKING_RATE_PER_MINUTE)

                // Charging cost
                .chargingCost(chargingCost)
                .powerConsumed(powerConsumed)

                // Warnings
                .warningMessage(warningMessage)
                .warningLevel(warningLevel)

                // Station info
                .stationName(stationName)
                .chargingPointName(chargingPointName)

                .build();
    }

    /**
     * Get parking status without caching (for admin/debugging)
     */
    public ParkingMonitorDTO getParkingStatusRealtime(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != Session.SessionStatus.PARKING) {
            throw new RuntimeException("Session is not in PARKING status");
        }

        return buildParkingMonitorDTO(session);
    }
}

