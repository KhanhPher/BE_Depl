package swp391.code.swp391.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.exception.UserStatusException;

/**
 * Utility class to check user status and throw appropriate exceptions
 * Can be used in service layer for additional status checks
 */
@Component
@RequiredArgsConstructor
public class UserStatusChecker {

    /**
     * Check if current authenticated user has ACTIVE status
     * @throws UserStatusException if user is BANNED or INACTIVE
     */
    public void requireActiveStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String status = jwt.getClaimAsString("status");

            if ("BANNED".equals(status)) {
                throw new UserStatusException(
                    User.UserStatus.BANNED,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin để biết thêm chi tiết.",
                    "USER_BANNED"
                );
            }

            if ("INACTIVE".equals(status)) {
                throw new UserStatusException(
                    User.UserStatus.INACTIVE,
                    "Tài khoản của bạn chưa được kích hoạt. Vui lòng kích hoạt tài khoản để sử dụng dịch vụ.",
                    "USER_INACTIVE"
                );
            }
        }
    }

    /**
     * Check if a specific user has ACTIVE status
     * @param user User to check
     * @throws UserStatusException if user is BANNED or INACTIVE
     */
    public void requireActiveStatus(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new UserStatusException(
                User.UserStatus.BANNED,
                "Tài khoản đã bị khóa. Không thể thực hiện thao tác này.",
                "USER_BANNED"
            );
        }

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new UserStatusException(
                User.UserStatus.INACTIVE,
                "Tài khoản chưa được kích hoạt. Không thể thực hiện thao tác này.",
                "USER_INACTIVE"
            );
        }
    }
}


