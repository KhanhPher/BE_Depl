package swp391.code.swp391.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.SessionProgressDTO;
import swp391.code.swp391.dto.SessionDTO;
import swp391.code.swp391.dto.StartSessionRequestDTO;
import swp391.code.swp391.util.JwtUtil;
import swp391.code.swp391.service.SessionService;

import java.text.ParseException;
import java.util.List;


@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final JwtUtil jwtUtil;

    // US10: POST /api/sessions/start
    @PostMapping("/start")
    public ResponseEntity<APIResponse<Long>> startSession(@Valid @RequestBody StartSessionRequestDTO request,
                                                          HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);
        Long sessionId;
        Long userId;
        try {
            userId = jwtUtil.getUserIdByTokenDecode(token);
            sessionId = sessionService.startSession(
                userId,
                request.getOrderId(),
                request.getVehicleId(),
                request.getUserLatitude(),
                request.getUserLongitude()
            );
        } catch (ParseException | JOSEException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error("Token parsing error"));
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(e.getMessage()));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Session started successfully", sessionId));
    }

    // US11: GET /api/sessions/{sessionId}/monitor
    // NOTE: SessionProgressScheduler is currently DISABLED
    // Client should poll this endpoint for session progress updates
    @GetMapping("/{sessionId}/monitor")
    public ResponseEntity<APIResponse<SessionProgressDTO>> monitorSession(@PathVariable Long sessionId,
                                                                          HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);
        Long userId;
        SessionProgressDTO progress;
        try {
            userId = jwtUtil.getUserIdByTokenDecode(token);
            progress = sessionService.monitorSession(sessionId, userId);
        } catch (ParseException | JOSEException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error("Token parsing error"));
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(e.getMessage()));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(e.getMessage()));
        }

        return ResponseEntity.ok(APIResponse.success("Session progress updated successfully", progress));
    }

    // US11: End charging session
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<APIResponse<Long>> endSession(@PathVariable Long sessionId,
                                                        HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);
        Long userId;
        try {
            userId = jwtUtil.getUserIdByTokenDecode(token);
            Long completedSessionId = sessionService.endSession(sessionId, userId);
            return ResponseEntity.ok(APIResponse.success("Session ended successfully", completedSessionId));
        } catch (ParseException | JOSEException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error("Token parsing error"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<APIResponse<List<SessionDTO>>> getAllSessions() {
        try {
            List<SessionDTO> list = sessionService.getAllSessions();
            return ResponseEntity.ok(APIResponse.success("Sessions retrieved", list));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<APIResponse<SessionDTO>> getSessionDetails(@PathVariable Long sessionId) {
        try {
            SessionDTO dto = sessionService.getSessionDetails(sessionId);
            return ResponseEntity.ok(APIResponse.success("Session details retrieved", dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<APIResponse<SessionDTO>> getSessionByOrderId(@PathVariable Long orderId) {
        try {
            SessionDTO dto = sessionService.getSessionByOrderId(orderId);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("No session found for order ID: " + orderId));
            }
            return ResponseEntity.ok(APIResponse.success("Session retrieved by order ID", dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/force-end")
    public ResponseEntity<APIResponse<Long>> forceEndSession(@PathVariable Long sessionId,
                                                             HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("Authorization");
        String token = jwtUtil.getTokenFromHeader(header);
        Long operatorId;
        try {
            operatorId = jwtUtil.getUserIdByTokenDecode(token);
            Long completed = sessionService.forceEndSession(sessionId, operatorId);
            return ResponseEntity.ok(APIResponse.success("Session force-ended", completed));
        } catch (ParseException | JOSEException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error("Token parsing error"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(e.getMessage()));
        }
    }
}