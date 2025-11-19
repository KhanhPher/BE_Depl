package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Notification;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class NotificationDTO {

    private Long notificationId;
    private Long userId;
    private String title;
    private String content;
    private String createdAt;
    private boolean isRead = false;
    private Notification.Type type;
}
