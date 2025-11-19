package swp391.code.swp391.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.NotificationDTO;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.util.JwtUtil;
import swp391.code.swp391.service.NotificationService;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(HttpServletRequest request) {
        User user;
        try {
            user = jwtUtil.getUserByTokenThroughSecurityContext();
            log.info("=== GET NOTIFICATIONS DEBUG ===");
            log.info("User ID: {}", user.getUserId());
            log.info("User Email: {}", user.getEmail());
        } catch (ParseException | JOSEException e) {
            log.error("JWT parsing error", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw new RuntimeException(e);
        }
        List<NotificationDTO> notifications = notificationService.getNotificationDTOs(user.getUserId());
        log.info("Notifications count returned: {}", notifications.size());
        log.info("Notifications: {}", notifications);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount() {
        User user;
        try {
            user = jwtUtil.getUserByTokenThroughSecurityContext();
            log.info("=== GET UNREAD COUNT DEBUG ===");
            log.info("User ID: {}", user.getUserId());
            log.info("User Email: {}", user.getEmail());
        } catch (ParseException | JOSEException e) {
            log.error("JWT parsing error in getUnreadCount", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Unexpected error in getUnreadCount", e);
            throw new RuntimeException(e);
        }
        Long count = notificationService.getUnreadCountForUser(user.getUserId());
        log.info("Unread count returned: {}", count);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        User user;
        try {
            user  = jwtUtil.getUserByTokenThroughSecurityContext();
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        notificationService.markAsRead(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        String token = jwtUtil.getTokenFromRequestHeader(request);
        Long userId;
        try {
            userId = jwtUtil.getUserIdByTokenDecode(token);
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
