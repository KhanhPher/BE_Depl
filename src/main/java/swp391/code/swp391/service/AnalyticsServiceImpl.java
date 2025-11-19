package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.repository.SessionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for Analytics and Dashboard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SessionRepository sessionRepository;

    @Override
    public List<ChargingSessionDTO> getChargingSessions(
            LocalDate startDate,
            LocalDate endDate,
            Long stationId,
            Long userId,
            String status) {

        // Set default dates if not provided
        LocalDateTime startDateTime = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusMonths(1);
        LocalDateTime endDateTime = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<Session> sessions;

        //filter by stationId or userId if provided
        if (stationId != null) {
            sessions = sessionRepository.findByOrderChargingPointStationStationIdAndStartTimeBetween(
                    stationId, startDateTime, endDateTime);
        } else if (userId != null) {
            sessions = sessionRepository.findByOrderUserUserIdAndStartTimeBetween(
                    userId, startDateTime, endDateTime);
        } else {
            sessions = sessionRepository.findByStartTimeBetween(startDateTime, endDateTime);
        }

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                Session.SessionStatus sessionStatus = Session.SessionStatus.valueOf(status.toUpperCase());
                sessions = sessions.stream()
                        .filter(s -> s.getStatus() == sessionStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid session status filter: {}", status);
            }
        }

        // Convert to DTOs
        return  sessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PeakHourDTO> getPeakHours(LocalDate startDate, LocalDate endDate, Long stationId) {

        // Get sessions for the period
        List<ChargingSessionDTO> sessions = getChargingSessions(startDate, endDate, stationId, null, null);

        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // Group sessions by hour
        Map<Integer, List<ChargingSessionDTO>> sessionsByHour = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().getHour()));

        // Calculate statistics for each hour
        List<PeakHourDTO> peakHours = new ArrayList<>();
        long totalDailySessions = sessions.size();

        for (int hour = 0; hour < 24; hour++) {
            List<ChargingSessionDTO> hourSessions = sessionsByHour.getOrDefault(hour, Collections.emptyList());

            long sessionCount = hourSessions.size();
            double totalEnergy = hourSessions.stream()
                    .mapToDouble(s -> s.getEnergyConsumed() != null ? s.getEnergyConsumed() : 0.0)
                    .sum();
            double totalRevenue = hourSessions.stream()
                    .mapToDouble(s -> s.getTotalCost() != null ? s.getTotalCost() : 0.0)
                    .sum();
            double averageEnergy = sessionCount > 0 ? totalEnergy / sessionCount : 0.0;

            String timeRange = String.format("%02d:00 - %02d:00", hour, (hour + 1) % 24);
            String peakLevel = determinePeakLevel(sessionCount, totalDailySessions);
            int percentageOfDaily = totalDailySessions > 0
                    ? (int) ((sessionCount * 100.0) / totalDailySessions)
                    : 0;

            PeakHourDTO peakHour = PeakHourDTO.builder()
                    .hour(hour)
                    .timeRange(timeRange)
                    .sessionCount(sessionCount)
                    .totalEnergy(Math.round(totalEnergy * 100.0) / 100.0)
                    .averageEnergy(Math.round(averageEnergy * 100.0) / 100.0)
                    .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                    .peakLevel(peakLevel)
                    .percentageOfDaily(percentageOfDaily)
                    .build();

            peakHours.add(peakHour);
        }

        // Sort by session count descending
        peakHours.sort((a, b) -> Long.compare(b.getSessionCount(), a.getSessionCount()));
        return peakHours;
    }

    @Override
    public List<UsageTrendDTO> getUsageTrends(String period, LocalDate startDate, LocalDate endDate, Long stationId) {

        // Get sessions for the period
        List<ChargingSessionDTO> sessions = getChargingSessions(startDate, endDate, stationId, null, null);

        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // Group sessions by date
        Map<LocalDate, List<ChargingSessionDTO>> sessionsByDate = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate()));

        List<UsageTrendDTO> trends = new ArrayList<>();

        // Calculate trends for each date
        List<LocalDate> dates = new ArrayList<>(sessionsByDate.keySet());
        dates.sort(LocalDate::compareTo);

        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            List<ChargingSessionDTO> dateSessions = sessionsByDate.get(date);

            long totalSessions = dateSessions.size();
            double totalEnergy = dateSessions.stream()
                    .mapToDouble(s -> s.getEnergyConsumed() != null ? s.getEnergyConsumed() : 0.0)
                    .sum();
            double totalRevenue = dateSessions.stream()
                    .mapToDouble(s -> s.getTotalCost() != null ? s.getTotalCost() : 0.0)
                    .sum();

            // Calculate average duration
            double averageDuration = dateSessions.stream()
                    .mapToDouble(s -> s.getDuration() != null ? s.getDuration() : 0.0)
                    .average()
                    .orElse(0.0);

            double averageEnergy = totalSessions > 0 ? totalEnergy / totalSessions : 0.0;

            // Count unique users
            int uniqueUsers = (int) dateSessions.stream()
                    .map(ChargingSessionDTO::getUserId)
                    .distinct()
                    .count();

            // Find peak hour for this day
            Integer peakHour = dateSessions.stream()
                    .collect(Collectors.groupingBy(s -> s.getStartTime().getHour(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            // Calculate growth rates (compared to previous period)
            Double sessionGrowthRate = null;
            Double revenueGrowthRate = null;
            Double energyGrowthRate = null;

            if (i > 0) {
                LocalDate prevDate = dates.get(i - 1);
                List<ChargingSessionDTO> prevSessions = sessionsByDate.get(prevDate);

                long prevTotalSessions = prevSessions.size();
                double prevTotalRevenue = prevSessions.stream()
                        .mapToDouble(s -> s.getTotalCost() != null ? s.getTotalCost() : 0.0)
                        .sum();
                double prevTotalEnergy = prevSessions.stream()
                        .mapToDouble(s -> s.getEnergyConsumed() != null ? s.getEnergyConsumed() : 0.0)
                        .sum();

                if (prevTotalSessions > 0) {
                    sessionGrowthRate = ((totalSessions - prevTotalSessions) * 100.0) / prevTotalSessions;
                }
                if (prevTotalRevenue > 0) {
                    revenueGrowthRate = ((totalRevenue - prevTotalRevenue) * 100.0) / prevTotalRevenue;
                }
                if (prevTotalEnergy > 0) {
                    energyGrowthRate = ((totalEnergy - prevTotalEnergy) * 100.0) / prevTotalEnergy;
                }
            }

            UsageTrendDTO trend = UsageTrendDTO.builder()
                    .date(date)
                    .period(period != null ? period.toUpperCase() : "DAILY")
                    .totalSessions(totalSessions)
                    .totalEnergy(Math.round(totalEnergy * 100.0) / 100.0)
                    .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                    .averageSessionDuration(Math.round(averageDuration * 100.0) / 100.0)
                    .averageEnergyPerSession(Math.round(averageEnergy * 100.0) / 100.0)
                    .uniqueUsers(uniqueUsers)
                    .peakHour(peakHour)
                    .sessionGrowthRate(sessionGrowthRate != null ? Math.round(sessionGrowthRate * 100.0) / 100.0 : null)
                    .revenueGrowthRate(revenueGrowthRate != null ? Math.round(revenueGrowthRate * 100.0) / 100.0 : null)
                    .energyGrowthRate(energyGrowthRate != null ? Math.round(energyGrowthRate * 100.0) / 100.0 : null)
                    .build();

            trends.add(trend);
        }
        return trends;
    }

    @Override
    public AnalyticsDashboardDTO getDashboard(LocalDate startDate, LocalDate endDate, Long stationId) {

        // Get all sessions for the period
        List<ChargingSessionDTO> allSessions = getChargingSessions(startDate, endDate, stationId, null, null);

        // Check if there's data
        if (allSessions.isEmpty()) {
            AnalyticsDashboardDTO dashboard = new AnalyticsDashboardDTO();
            dashboard.setMessage("No data available for dashboard");
            dashboard.setHasData(false);
            return dashboard;
        }

        // Calculate summary statistics
        long totalSessions = allSessions.size();
        double totalEnergy = allSessions.stream()
                .mapToDouble(s -> s.getEnergyConsumed() != null ? s.getEnergyConsumed() : 0.0)
                .sum();
        double totalRevenue = allSessions.stream()
                .mapToDouble(s -> s.getTotalCost() != null ? s.getTotalCost() : 0.0)
                .sum();
        int activeUsers = (int) allSessions.stream()
                .map(ChargingSessionDTO::getUserId)
                .distinct()
                .count();
        int activeStations = (int) allSessions.stream()
                .map(ChargingSessionDTO::getStationId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Get recent sessions (last 10)
        List<ChargingSessionDTO> recentSessions = getRecentSessions(10);

        // Get peak hours analysis
        List<PeakHourDTO> peakHours = getPeakHours(startDate, endDate, stationId);

        // Get usage trends
        List<UsageTrendDTO> trends = getUsageTrends("DAILY", startDate, endDate, stationId);

        AnalyticsDashboardDTO dashboard = AnalyticsDashboardDTO.builder()
                .hasData(true)
                .message("Dữ liệu analytics đã được tải thành công")
                .totalSessions(totalSessions)
                .totalEnergy(Math.round(totalEnergy * 100.0) / 100.0)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .activeUsers(activeUsers)
                .activeStations(activeStations)
                .recentSessions(recentSessions)
                .peakHours(peakHours)
                .trends(trends)
                .build();

        return dashboard;
    }

    @Override
    public List<ChargingSessionDTO> getRecentSessions(int limit) {
        List<Session> sessions = sessionRepository.findTop10ByOrderByStartTimeDesc();

        List<ChargingSessionDTO> result = sessions.stream()
                .limit(limit)
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());
        return result;
    }

    // Helper methods

    private ChargingSessionDTO convertToSessionDTO(Session session) {
        if (session == null) {
            return null;
        }

        // Calculate duration
        Integer duration = null;
        if (session.getEndTime() != null && session.getStartTime() != null) {
            duration = (int) java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        }

        // Calculate total cost (base cost + fees)
        double totalCost = session.getBaseCost() != null ? session.getBaseCost() : 0.0;
        if (session.getFees() != null) {
            totalCost += session.getFees().stream()
                    .mapToDouble(fee -> fee.getAmount() != null ? fee.getAmount() : 0.0)
                    .sum();
        }

        ChargingSessionDTO dto = ChargingSessionDTO.builder()
                .sessionId(session.getSessionId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .energyConsumed(session.getPowerConsumed())
                .totalCost(Math.round(totalCost * 100.0) / 100.0)
                .status(session.getStatus() != null ? session.getStatus().name() : null)
                .duration(duration)
                .build();

        // Add order-related information if available
        if (session.getOrder() != null) {
            dto.setUserId(session.getOrder().getUser() != null ? session.getOrder().getUser().getUserId() : null);
            dto.setUserName(session.getOrder().getUser() != null ? session.getOrder().getUser().getFullName() : null);

            if (session.getOrder().getChargingPoint() != null) {
                dto.setChargingPointId(session.getOrder().getChargingPoint().getChargingPointId());

                if (session.getOrder().getChargingPoint().getConnectorType() != null) {
                    dto.setConnectorType(session.getOrder().getChargingPoint().getConnectorType().getTypeName());
                }

                if (session.getOrder().getChargingPoint().getStation() != null) {
                    dto.setStationId(session.getOrder().getChargingPoint().getStation().getStationId());
                    dto.setStationName(session.getOrder().getChargingPoint().getStation().getStationName());
                }
            }

            if (session.getOrder().getVehicle() != null && session.getOrder().getVehicle().getCarModel() != null) {
                dto.setVehicleModel(session.getOrder().getVehicle().getCarModel().getModel());
            }
        }
        return dto;
    }

    private String determinePeakLevel(long sessionCount, long totalSessions) {
        if (totalSessions == 0) {
            return "LOW";
        }

        double percentage = (sessionCount * 100.0) / totalSessions;

        if (percentage >= 8.0) { // >= 8% of daily total (significantly above average 4.17%)
            return "VERY_HIGH";
        } else if (percentage >= 6.0) {
            return "HIGH";
        } else if (percentage >= 4.0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}

