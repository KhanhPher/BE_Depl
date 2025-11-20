package swp391.code.swp391.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.SessionProgressDTO;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.SessionRepository;
import swp391.code.swp391.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * TEMPORARILY DISABLED - Not using scheduler + websocket combination for now
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionProgressScheduler {

    private final SessionRepository sessionRepository;
    private final VehicleRepository vehicleRepository;
    // private final SessionWebSocketService sessionWebSocketService;

    // DISABLED: Temporarily not using scheduler for session progress
    // @Scheduled(fixedRate = 1000*5) // 5 seconds
    public void pushActiveSessionsProgress() {
        try {
            // Find all active charging sessions
            List<Session> activeSessions = sessionRepository.findByStatus(Session.SessionStatus.CHARGING);

            if (activeSessions.isEmpty()) {
                return; // No active sessions
            }

            log.debug("Auto-pushing progress for {} active sessions", activeSessions.size());

            for (Session session : activeSessions) {
                try {
                    pushSessionProgress(session);
                } catch (Exception e) {
                    log.error("Failed to push progress for session {}: {}",
                        session.getSessionId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in session progress scheduler: {}", e.getMessage());
        }
    }

    private void pushSessionProgress(Session session) {
        Order order = session.getOrder();
        if (order == null || order.getUser() == null) {
            return;
        }

        ConnectorType connectorType = order.getChargingPoint().getConnectorType();
        Vehicle vehicle = vehicleRepository.findById(order.getVehicle().getId()).orElse(null);
        if (vehicle == null) {
            return;
        }

        // Calculate progress
        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), now);
        long minutesElapsed = secondsElapsed / 60;

        double power = connectorType.getPowerOutput(); // kW
        double powerConsumed = power * (secondsElapsed / 3600.0); // Tính chính xác từ giây

        double basePrice = connectorType.getPricePerKWh();
        double cost = powerConsumed * basePrice;

        double currentBattery = calculateBatteryPercentage(vehicle, powerConsumed) + order.getStartedBattery();
        if (currentBattery > 100) {
            currentBattery = 100.0;
        }

        long estimatedTotalMinutes = expectedMinutes(vehicle, order.getExpectedBattery(), power);
        long remainingMinutes = estimatedTotalMinutes > minutesElapsed
            ? estimatedTotalMinutes - minutesElapsed
            : 0L;

        // Calculate progress percentage
        double startBattery = order.getStartedBattery();
        double targetBattery = order.getExpectedBattery();
        double progressPercentage = 0.0;
        if (targetBattery > startBattery) {
            progressPercentage = ((currentBattery - startBattery) / (targetBattery - startBattery)) * 100.0;
            if (progressPercentage > 100) progressPercentage = 100.0;
            if (progressPercentage < 0) progressPercentage = 0.0;
        }

        // Build DTO
        SessionProgressDTO dto = new SessionProgressDTO(
            startBattery,           // startBattery
            currentBattery,         // currentBattery
            targetBattery,          // targetBattery
            progressPercentage,     // progressPercentage
            powerConsumed,          // powerConsumed
            cost,                   // cost
            secondsElapsed,         // elapsedSeconds
            minutesElapsed,         // elapsedMinutes
            remainingMinutes,       // estimatedRemainingMinutes
            session.getStartTime(), // startTime
            now,                    // currentTime
            session.getStatus().name() // status
        );

        // Push via WebSocket
        // sessionWebSocketService.sendSessionProgressToUser(order.getUser(), dto);

        log.debug("Pushed progress for session {}: {}% battery, {} minutes elapsed",
            session.getSessionId(), currentBattery, minutesElapsed);
    }

    /**
     * Calculate battery percentage from kWh consumed
     */
    private Double calculateBatteryPercentage(Vehicle vehicle, Double kwh) {
        if (vehicle == null || vehicle.getCarModel() == null) {
            return 0.0;
        }
        Double capacity = vehicle.getCarModel().getCapacity();
        if (capacity == null || capacity == 0) {
            return 0.0;
        }
        return (kwh / capacity) * 100;
    }

    /**
     * Calculate expected minutes to reach target battery
     */
    private long expectedMinutes(Vehicle vehicle, Double expectedBattery, double powerKw) {
        if (vehicle == null || vehicle.getCarModel() == null || expectedBattery == null) {
            return 0L;
        }
        Double capacity = vehicle.getCarModel().getCapacity();
        if (capacity == null || capacity == 0 || powerKw == 0) {
            return 0L;
        }

        double batteryToCharge = expectedBattery; // assuming started from 0 for simplicity
        double kwhNeeded = (batteryToCharge / 100.0) * capacity;
        double hoursNeeded = kwhNeeded / powerKw;

        return Math.round(hoursNeeded * 60); // convert to minutes
    }
}
