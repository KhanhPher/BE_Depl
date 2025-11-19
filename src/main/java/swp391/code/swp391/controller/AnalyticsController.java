package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.service.AnalyticsService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<List<ChargingSessionDTO>>> getChargingSessions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {

        try {
            log.info("Request to get charging sessions - startDate: {}, endDate: {}, stationId: {}, userId: {}, status: {}",
                    startDate, endDate, stationId, userId, status);

            List<ChargingSessionDTO> sessions = analyticsService.getChargingSessions(
                    startDate, endDate, stationId, userId, status);

            String message = sessions.isEmpty()
                    ? "Không có phiên sạc nào trong khoảng thời gian này"
                    : "Lấy danh sách phiên sạc thành công";

            APIResponse<List<ChargingSessionDTO>> response = APIResponse.<List<ChargingSessionDTO>>builder()
                    .success(true)
                    .message(message)
                    .data(sessions)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting charging sessions: {}", e.getMessage(), e);

            APIResponse<List<ChargingSessionDTO>> response = APIResponse.<List<ChargingSessionDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy danh sách phiên sạc: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/peak-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<List<PeakHourDTO>>> getPeakHours(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long stationId) {

        try {
            // Set default dates if not provided (last 7 days)
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(7);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<PeakHourDTO> peakHours = analyticsService.getPeakHours(startDate, endDate, stationId);

            String message = peakHours.isEmpty()
                    ? "Không có dữ liệu khung giờ cao điểm trong khoảng thời gian này"
                    : "Phân tích khung giờ cao điểm thành công";

            APIResponse<List<PeakHourDTO>> response = APIResponse.<List<PeakHourDTO>>builder()
                    .success(true)
                    .message(message)
                    .data(peakHours)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting peak hours: {}", e.getMessage(), e);

            APIResponse<List<PeakHourDTO>> response = APIResponse.<List<PeakHourDTO>>builder()
                    .success(false)
                    .message("Lỗi khi phân tích khung giờ cao điểm: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<List<UsageTrendDTO>>> getUsageTrends(
            @RequestParam(required = false, defaultValue = "DAILY") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long stationId) {

        try {
            log.info("Request to get usage trends - period: {}, startDate: {}, endDate: {}, stationId: {}",
                    period, startDate, endDate, stationId);

            // Set default dates if not provided (last 30 days)
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            List<UsageTrendDTO> trends = analyticsService.getUsageTrends(period, startDate, endDate, stationId);

            String message = trends.isEmpty()
                    ? "Không có dữ liệu xu hướng sử dụng trong khoảng thời gian này"
                    : "Phân tích xu hướng sử dụng thành công";

            APIResponse<List<UsageTrendDTO>> response = APIResponse.<List<UsageTrendDTO>>builder()
                    .success(true)
                    .message(message)
                    .data(trends)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting usage trends: {}", e.getMessage(), e);

            APIResponse<List<UsageTrendDTO>> response = APIResponse.<List<UsageTrendDTO>>builder()
                    .success(false)
                    .message("Lỗi khi phân tích xu hướng sử dụng: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<AnalyticsDashboardDTO>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long stationId) {

        try {
            log.info("Request to get analytics dashboard - startDate: {}, endDate: {}, stationId: {}",
                    startDate, endDate, stationId);

            // Set default dates if not provided (last 30 days)
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            AnalyticsDashboardDTO dashboard = analyticsService.getDashboard(startDate, endDate, stationId);

            APIResponse<AnalyticsDashboardDTO> response = APIResponse.<AnalyticsDashboardDTO>builder()
                    .success(dashboard.getHasData())
                    .message(dashboard.getMessage())
                    .data(dashboard)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting dashboard: {}", e.getMessage(), e);

            APIResponse<AnalyticsDashboardDTO> response = APIResponse.<AnalyticsDashboardDTO>builder()
                    .success(false)
                    .message("Lỗi khi tải dashboard: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/recent-sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<List<ChargingSessionDTO>>> getRecentSessions(
            @RequestParam(required = false, defaultValue = "10") int limit) {

        try {
            log.info("Request to get {} recent sessions", limit);

            // Validate limit
            if (limit < 1 || limit > 100) {
                limit = 10;
            }

            List<ChargingSessionDTO> sessions = analyticsService.getRecentSessions(limit);

            String message = sessions.isEmpty()
                    ? "Không có phiên sạc gần đây"
                    : "Lấy danh sách phiên sạc gần đây thành công";

            APIResponse<List<ChargingSessionDTO>> response = APIResponse.<List<ChargingSessionDTO>>builder()
                    .success(true)
                    .message(message)
                    .data(sessions)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting recent sessions: {}", e.getMessage(), e);

            APIResponse<List<ChargingSessionDTO>> response = APIResponse.<List<ChargingSessionDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy phiên sạc gần đây: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

