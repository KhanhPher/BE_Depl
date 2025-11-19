package swp391.code.swp391.service;

import swp391.code.swp391.dto.AnalyticsDashboardDTO;
import swp391.code.swp391.dto.ChargingSessionDTO;
import swp391.code.swp391.dto.PeakHourDTO;
import swp391.code.swp391.dto.UsageTrendDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Analytics and Dashboard
 * Provides charging session history, peak hours analysis, and usage trends
 */
public interface AnalyticsService {

    /**
     * Get all charging sessions with optional filters
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param stationId Station ID filter (optional)
     * @param userId User ID filter (optional)
     * @param status Session status filter (optional)
     * @return List of charging sessions or empty list if no data
     */
    List<ChargingSessionDTO> getChargingSessions(
            LocalDate startDate,
            LocalDate endDate,
            Long stationId,
            Long userId,
            String status
    );

    /**
     * Get peak hours analysis
     * @param startDate Start date for analysis
     * @param endDate End date for analysis
     * @param stationId Station ID filter (optional)
     * @return List of peak hours data or empty list if no data
     */
    List<PeakHourDTO> getPeakHours(LocalDate startDate, LocalDate endDate, Long stationId);

    /**
     * Get usage trends
     * @param period Period type: DAILY, WEEKLY, MONTHLY
     * @param startDate Start date for trends
     * @param endDate End date for trends
     * @param stationId Station ID filter (optional)
     * @return List of usage trends or empty list if no data
     */
    List<UsageTrendDTO> getUsageTrends(String period, LocalDate startDate, LocalDate endDate, Long stationId);

    /**
     * Get complete analytics dashboard
     * @param startDate Start date for dashboard
     * @param endDate End date for dashboard
     * @param stationId Station ID filter (optional)
     * @return Dashboard with all analytics data
     */
    AnalyticsDashboardDTO getDashboard(LocalDate startDate, LocalDate endDate, Long stationId);

    /**
     * Get recent charging sessions (last 10)
     * @param limit Number of sessions to return
     * @return List of recent sessions or empty list if no data
     */
    List<ChargingSessionDTO> getRecentSessions(int limit);
}

