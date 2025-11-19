package swp391.code.swp391.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.NotificationDTO;
import swp391.code.swp391.dto.NotificationSignalDTO;
import swp391.code.swp391.dto.StaffDTO;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.NotificationRepository;
import swp391.code.swp391.repository.OrderRepository;
import swp391.code.swp391.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    // private final SimpMessagingTemplate simpMessagingTemplate;
    private final StaffManagementService staffManagementService;
    @Override
    @Transactional
    public List<NotificationDTO> getNotificationDTOs(Long userId) {
        log.info("=== getNotificationDTOs SERVICE DEBUG ===");
        log.info("Looking for notifications for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User found: {} (email: {})", user.getUserId(), user.getEmail());

        List<Notification> notifications = notificationRepository.findByUserOrderBySentTimeDesc(user);
        log.info("Raw notifications from DB: {}", notifications.size());

        if (notifications.isEmpty()) {
            log.warn("No notifications found for user {}", userId);
        } else {
            log.info("First notification: {}", notifications.get(0));
        }

        return notifications.stream()
                .map(n -> new NotificationDTO(n.getNotificationId(), n.getUser().getUserId(),
                        n.getTitle(), n.getContent(),
                        n.getSentTime().toString(),
                        n.getIsRead(), n.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findUnreadByUser(user);
    }

    @Override
    public Long getUnreadCountForUser(Long userId) {
        log.info("=== getUnreadCountForUser SERVICE DEBUG ===");
        log.info("Counting unread notifications for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User found: {} (email: {})", user.getUserId(), user.getEmail());

        Long count = notificationRepository.countUnreadByUser(user);
        log.info("Unread count from DB: {}", count);

        return count;
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        int updated = notificationRepository.markAsRead(notificationId, user);
        if (updated == 0) {
            throw new RuntimeException("Notification not found or not owned by user");
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationRepository.markAllAsRead(user);
    }

    public void pushNotificationToUser(Notification notification, Notification.Type type) {
        // Lấy username (email hoặc phone) của user nhận notification
        User recipientUser = notification.getUser();
        String username = recipientUser.getEmail() != null ? recipientUser.getEmail() : recipientUser.getPhone();

        if (username == null) {
            throw new RuntimeException("User has no email or phone to send notification");
        }

        NotificationSignalDTO dto = new NotificationSignalDTO();
        dto.setType(type);
        dto.setNotificationCount(getUnreadCountForUser(recipientUser.getUserId()).intValue());

        // Gửi notification đến user cụ thể qua WebSocket
//        simpMessagingTemplate.convertAndSendToUser(
//                username,
//                "/queue/notifications",
//                dto
//        );
    }

    public void pushNotificationToAll(Notification notification) {
        // Gửi broadcast notification đến tất cả users đang subscribe /topic/notifications
//        simpMessagingTemplate.convertAndSend("/topic/notifications", convertToDTO(notification));
    }

//=====================BOOKING NOTIFICATION==========================
    public enum NotificationEvent {
        BOOKING_SUCCESS, CANCEL_ORDER, SESSION_START, SESSION_COMPLETE, SESSION_WAITING_STAFF_DECISION, SESSION_STILL_PARKED
    }

    @Transactional
    public void createBookingOrderNotification(Long orderId, NotificationEvent event, String additionalInfo) {
        // Kiểm tra Order tồn tại
        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }

        User user = order.getUser();
        String stationName = order.getChargingPoint().getStation().getStationName();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.Type.BOOKING);
        notification.setSentTime(LocalDateTime.now());

        // Tùy chỉnh title và content theo sự kiện
        switch (event) {
            case BOOKING_SUCCESS:
                notification.setTitle("Đặt chỗ thành công!");
                notification.setContent(String.format(
                        "Đặt chỗ của bạn tại trạm %s đã được xác nhận.\nOrder ID: %d\nThời gian: %s",
                        stationName, order.getOrderId(), order.getStartTime()
                ));
                break;
            case CANCEL_ORDER:
                notification.setTitle("Hủy đặt chỗ");
                notification.setContent(String.format(
                        "Đặt chỗ tại trạm %s đã bị hủy.\nOrder ID: %d\n%s",
                        stationName, order.getOrderId(), additionalInfo
                ));
                break;
            case SESSION_START:
                notification.setTitle("Phiên sạc bắt đầu");
                notification.setContent(String.format(
                        "Phiên sạc của bạn tại trạm %s đã bắt đầu.\nOrder ID: %d\nThời gian: %s",
                        stationName, order.getOrderId(), LocalDateTime.now()
                ));
                break;
            case SESSION_COMPLETE:
                notification.setTitle("Phiên sạc hoàn tất - Vui lòng rời trạm");
                notification.setContent(String.format(
                        "Phiên sạc tại trạm %s đã hoàn tất.\n" +
                        "VUI LÒNG RỜI KHỎI TRẠM NGAY để tránh bị tính phí đỗ xe (500 VND/phút, tăng dần theo thời gian)\n" +
                        "Order ID: %d\n" +
                        "Thời gian kết thúc: %s\n" +
                        "%s",
                        stationName, order.getOrderId(), LocalDateTime.now(),
                        additionalInfo != null ? additionalInfo : ""
                ));
                break;
            case SESSION_WAITING_STAFF_DECISION:
                notification.setTitle("Chờ xác nhận rời trạm");
                notification.setContent(String.format(
                        "Phiên sạc tại trạm %s đã kết thúc.\nVui lòng xác nhận đã rời trạm để tránh phí đỗ xe.\nOrder ID: %d",
                        stationName, order.getOrderId()
                ));
                break;
            case SESSION_STILL_PARKED:
                notification.setTitle("Xe vẫn đậu tại trạm");
                notification.setContent(String.format(
                        "Xe của bạn vẫn đậu tại trạm %s sau khi sạc xong.\nPhí đỗ xe đang được tính.\nOrder ID: %d",
                        stationName, order.getOrderId()
                ));
                break;
            default:
                throw new IllegalArgumentException("Invalid notification event: " + event);
        }
        notificationRepository.save(notification);

        // Push via WebSocket
        pushNotificationToUser(notification,Notification.Type.BOOKING);
    }

    //=====================PAYMENT NOTIFICATION==========================
    public enum PaymentEvent {
        PAYMENT_SUCCESS, PAYMENT_FAILED, WALLET_TOPUP
    }

    @Transactional
    public void createPaymentNotification(Long userId, PaymentEvent event, Double amount, String additionalInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.Type.PAYMENT);
        notification.setSentTime(LocalDateTime.now());

        switch (event) {
            case PAYMENT_SUCCESS:
                notification.setTitle("Thanh toán thành công");
                notification.setContent(String.format(
                        "Thanh toán thành công số tiền: %.0f VND\n%s",
                        amount, additionalInfo != null ? additionalInfo : ""
                ));
                break;
            case PAYMENT_FAILED:
                notification.setTitle("Thanh toán thất bại");
                notification.setContent(String.format(
                        "Thanh toán thất bại số tiền: %.0f VND\nLý do: %s",
                        amount, additionalInfo != null ? additionalInfo : "Lỗi hệ thống"
                ));
                break;
            case WALLET_TOPUP:
                notification.setTitle("Nạp tiền vào ví thành công");
                notification.setContent(String.format(
                        "Đã nạp thành công %.0f VND vào ví của bạn\n%s",
                        amount, additionalInfo != null ? additionalInfo : ""
                ));
                break;
            default:
                throw new IllegalArgumentException("Invalid payment event: " + event);
        }
        notificationRepository.save(notification);

//        // Push via WebSocket
//        pushNotificationToUser(notification,Notification.Type.PAYMENT);
//
//        // Push updated unread count after creating notification
//        Long updatedCount = getUnreadCountForUser(user.getUserId());
//        pushUnreadCountToUser(user, updatedCount);
    }

    //=====================ISSUE NOTIFICATION==========================
    public enum IssueEvent {
        STATION_ERROR_ADMIN, STATION_ERROR_STAFF, CHARGING_POINT_CHANGE_DRIVER
    }

    @Transactional
    public void createIssueNotification(Long stationId, IssueEvent event, String additionalInfo) {
        // Tìm tất cả user liên quan (admin, staff, driver)
        List<User> targetUsers = new ArrayList<>();

        switch (event) {
            case STATION_ERROR_ADMIN:
                // Thông báo cho admin
                targetUsers = userRepository.findByRole(User.UserRole.ADMIN);
                break;
            case STATION_ERROR_STAFF:
                // Thông báo cho staff của station
                List<StaffDTO> staffs = staffManagementService.getStaffByStation(stationId);
                targetUsers = staffs.stream()
                        .map(staffDTO -> userRepository.findById(staffDTO.getUserId())
                                .orElseThrow(() -> new RuntimeException("Staff user not found with ID: " + staffDTO.getUserId())))
                        .collect(Collectors.toList());
                break;
            case CHARGING_POINT_CHANGE_DRIVER:
                // Thông báo cho driver về việc đổi charging point
                // TODO: Implement logic to find affected drivers
                break;
        }

        for (User user : targetUsers) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(Notification.Type.ISSUE);
            notification.setSentTime(LocalDateTime.now());

            switch (event) {
                case STATION_ERROR_ADMIN:
                    notification.setTitle("Lỗi trạm sạc - Cần xử lý");
                    notification.setContent(String.format(
                            "Trạm sạc ID: %d gặp sự cố\nChi tiết: %s\nVui lòng kiểm tra và xử lý",
                            stationId, additionalInfo
                    ));
                    break;
                case STATION_ERROR_STAFF:
                    notification.setTitle("Trạm sạc gặp sự cố");
                    notification.setContent(String.format(
                            "Trạm sạc ID: %d đang gặp sự cố\nChi tiết: %s",
                            stationId, additionalInfo
                    ));
                    break;
                case CHARGING_POINT_CHANGE_DRIVER:
                    notification.setTitle("Thay đổi điểm sạc");
                    notification.setContent(String.format(
                            "Điểm sạc của bạn đã được thay đổi\nChi tiết: %s",
                            additionalInfo
                    ));
                    break;
            }
            notificationRepository.save(notification);
            // Push via WebSocket
            pushNotificationToUser(notification,Notification.Type.ISSUE);
        }
    }

    //=====================PENALTY NOTIFICATION==========================
    public enum PenaltyEvent {
        NO_SHOW_PENALTY, CANCEL_PENALTY, OVERTIME_PENALTY, PARKING_PENALTY
    }

    @Transactional
    public void createPenaltyNotification(Long orderId, PenaltyEvent event, Double penaltyAmount, String reason) {
        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }

        User user = order.getUser();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.Type.PENALTY);
        notification.setSentTime(LocalDateTime.now());

        switch (event) {
            case NO_SHOW_PENALTY:
                notification.setTitle("Phạt không đến đúng giờ");
                notification.setContent(String.format(
                        "Bạn đã bị phạt %.0f VND vì không đến đúng giờ đặt chỗ\nOrder ID: %d\nLý do: %s",
                        penaltyAmount, orderId, reason
                ));
                break;
            case CANCEL_PENALTY:
                notification.setTitle("Phạt hủy đặt chỗ");
                notification.setContent(String.format(
                        "Bạn đã bị phạt %.0f VND vì hủy đặt chỗ\nOrder ID: %d\nLý do: %s",
                        penaltyAmount, orderId, reason
                ));
                break;
            case OVERTIME_PENALTY:
                notification.setTitle("Phạt sạc quá giờ");
                notification.setContent(String.format(
                        "Bạn đã bị phạt %.0f VND vì sạc quá thời gian quy định\nOrder ID: %d\nLý do: %s",
                        penaltyAmount, orderId, reason
                ));
                break;
            case PARKING_PENALTY:
                notification.setTitle("Phí đỗ xe sau khi sạc");
                notification.setContent(String.format(
                        "Bạn đã bị tính phí %.0f VND vì đỗ xe tại trạm sau khi sạc xong\nOrder ID: %d\nLý do: %s",
                        penaltyAmount, orderId, reason
                ));
                break;
            default:
                throw new IllegalArgumentException("Invalid penalty event: " + event);
        }
        notificationRepository.save(notification);

//        // Push via WebSocket
//        pushNotificationToUser(notification,Notification.Type.PENALTY);
//
//        // Push updated unread count after creating notification
//        Long updatedCount = getUnreadCountForUser(user.getUserId());
//        pushUnreadCountToUser(user, updatedCount);
    }

    //=====================GENERAL NOTIFICATION==========================
    @Transactional
    public void createGeneralNotification(List<Long> userIds, String title, String content) {
//        for (Long userId : userIds) {
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//
//            Notification notification = new Notification();
//            notification.setUser(user);
//            notification.setType(Notification.Type.GENERAL);
//            notification.setTitle(title);
//            notification.setContent(content);
//            notification.setSentTime(LocalDateTime.now());
//            notificationRepository.save(notification);
//            // Push via WebSocket
//            pushNotificationToUser(notification);
//        }
    }

    @Transactional
    public void createGeneralNotificationForAllUsers(String title, String content) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(Notification.Type.GENERAL);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setSentTime(LocalDateTime.now());
            notificationRepository.save(notification);

            // Push via WebSocket
            pushNotificationToAll(notification);
        }
    }

//    // Push unread count to user via WebSocket
//    public void pushUnreadCountToUser(User user, Long count) {
//        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();
//        if (username == null) {
//            log.warn("User {} has no email or phone to send notification count", user.getUserId());
//            return;
//        }
//
//        NotificationSignalDTO dto = new NotificationSignalDTO();
//        dto.setType(Notification.Type.GENERAL); // Hoặc type phù hợp
//        dto.setNotificationCount(count.intValue());
//    }

    // Helper method to convert Notification to NotificationDTO
    private NotificationDTO convertToDTO(Notification notification) {
        return new NotificationDTO(
            notification.getNotificationId(),
            notification.getUser().getUserId(),
            notification.getTitle(),
            notification.getContent(),
            notification.getSentTime().toString(),
            notification.getIsRead(),
            notification.getType()
        );
    }
}
