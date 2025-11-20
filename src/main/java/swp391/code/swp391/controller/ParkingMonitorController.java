package swp391.code.swp391.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.ParkingMonitorDTO;
import swp391.code.swp391.service.ParkingMonitorService;
import swp391.code.swp391.util.JwtUtil;

import java.text.ParseException;

/**
 * REST API for parking duration monitoring
 * Designed for polling-based updates (without WebSocket)
 */
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
@Slf4j
public class ParkingMonitorController {

    private final ParkingMonitorService parkingMonitorService;
    private final JwtUtil jwtUtil;

    /**
     * Poll parking status for a session
     * GET /api/parking/monitor/{sessionId}
     *
     * Frontend should poll this every 30-60 seconds
     * Response is cached for 30s to reduce DB load
     *
     * @param sessionId Session ID to monitor
     * @param httpServletRequest HTTP request (for JWT extraction)
     * @return Parking monitor DTO with real-time info
     */
    @GetMapping("/monitor/{sessionId}")
    public ResponseEntity<APIResponse<ParkingMonitorDTO>> monitorParking(
            @PathVariable Long sessionId,
            HttpServletRequest httpServletRequest) {

        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);

        try {
            Long userId = jwtUtil.getUserIdByTokenDecode(token);

            ParkingMonitorDTO status = parkingMonitorService.getParkingStatus(sessionId, userId);

            return ResponseEntity.ok(APIResponse.success(
                    "Parking status retrieved successfully",
                    status));

        } catch (ParseException | JOSEException e) {
            log.error("Token parsing error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error("Invalid token"));

        } catch (RuntimeException e) {
            log.error("Error monitoring parking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Internal server error"));
        }
    }

    /**
     * Get parking status without cache (for testing/debugging)
     * GET /api/parking/monitor/{sessionId}/realtime
     *
     * @param sessionId Session ID
     * @return Real-time parking status
     */
    @GetMapping("/monitor/{sessionId}/realtime")
    public ResponseEntity<APIResponse<ParkingMonitorDTO>> monitorParkingRealtime(
            @PathVariable Long sessionId,
            HttpServletRequest httpServletRequest) {

        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);

        try {
            // Still validate user but don't check authorization (for debugging)
            jwtUtil.getUserIdByTokenDecode(token);

            ParkingMonitorDTO status = parkingMonitorService.getParkingStatusRealtime(sessionId);

            return ResponseEntity.ok(APIResponse.success(
                    "Real-time parking status retrieved",
                    status));

        } catch (ParseException | JOSEException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error("Invalid token"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Internal server error"));
        }
    }
}

