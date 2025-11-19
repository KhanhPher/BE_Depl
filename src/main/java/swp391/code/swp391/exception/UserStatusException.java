package swp391.code.swp391.exception;

import lombok.Getter;
import swp391.code.swp391.entity.User;

/**
 * Exception thrown when a user's status prevents them from performing an action
 */
@Getter
public class UserStatusException extends RuntimeException {
    private final User.UserStatus userStatus;
    private final String reason;

    public UserStatusException(User.UserStatus status, String message) {
        super(message);
        this.userStatus = status;
        this.reason = message;
    }

    public UserStatusException(User.UserStatus status, String message, String reason) {
        super(message);
        this.userStatus = status;
        this.reason = reason;
    }
}

