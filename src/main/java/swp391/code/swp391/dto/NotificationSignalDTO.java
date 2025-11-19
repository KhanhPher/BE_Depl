package swp391.code.swp391.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import swp391.code.swp391.entity.Notification;

@Data
@RequiredArgsConstructor
public class NotificationSignalDTO { // DTO trả về để FE biết có sự thay đổi

    private Notification.Type type;
    private int notificationCount;
}
