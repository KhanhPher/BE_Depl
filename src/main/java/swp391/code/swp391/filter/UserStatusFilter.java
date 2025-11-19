package swp391.code.swp391.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import swp391.code.swp391.dto.APIResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserStatusFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();

        // Always skip filter for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip for read-only endpoints (allowed for all authenticated users)
        if (isReadOnlyEndpoint(requestURI, httpMethod)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String status = jwt.getClaimAsString("status");

            // Check if this is a main flow endpoint that requires ACTIVE status
            if (isMainFlowEndpoint(requestURI)) {
                // Kiểm tra nếu user bị banned
                if ("BANNED".equals(status)) {
                    sendBannedUserResponse(response);
                    return;
                }

                // Kiểm tra nếu user inactive
                if ("INACTIVE".equals(status)) {
                    sendInactiveUserResponse(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Public endpoints that don't require authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/otp/") ||
               requestURI.startsWith("/api/payment/") ||
               requestURI.startsWith("/api/test/") ||
               requestURI.startsWith("/api/notifications/connection/ws/");
    }

    /**
     * Read-only endpoints that are allowed for all authenticated users regardless of status
     * Users can view information but cannot perform actions
     */
    private boolean isReadOnlyEndpoint(String requestURI, String httpMethod) {
        // Allow GET requests to view information
        if ("GET".equals(httpMethod)) {
            return requestURI.startsWith("/api/users/") ||
                   requestURI.startsWith("/api/charging-stations") ||
                   requestURI.startsWith("/api/connector-types") ||
                   requestURI.startsWith("/api/car-models") ||
                   requestURI.startsWith("/api/subscriptions") ||
                   requestURI.startsWith("/api/subscription-features") ||
                   requestURI.startsWith("/api/notifications") ||
                   requestURI.matches("/api/vehicles/user/\\d+"); // Allow viewing own vehicles
        }
        return false;
    }

    /**
     * Main flow endpoints that require ACTIVE user status
     * These are operations that BANNED/INACTIVE users should not be able to perform
     */
    private boolean isMainFlowEndpoint(String requestURI) {
        return requestURI.startsWith("/api/orders") ||           // Booking/ordering charging slots
               requestURI.startsWith("/api/sessions") ||         // Starting/managing charging sessions
               requestURI.startsWith("/api/vehicles") ||         // Managing vehicles (except viewing)
               requestURI.startsWith("/api/issue-reports") ||    // Creating issue reports
               requestURI.startsWith("/api/transactions") ||     // Transaction operations
               requestURI.matches("/api/users/\\d+/update") ||   // Updating user profile (except viewing)
               requestURI.matches("/api/subscriptions/.*/subscribe"); // Subscribing to plans
    }


    private void sendInactiveUserResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("reason", "USER_INACTIVE");

        APIResponse<Map<String, Object>> apiResponse = APIResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Tài khoản của bạn chưa được kích hoạt")
                .data(errorData)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private void sendBannedUserResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("reason", "USER_BANNED");

        APIResponse<Map<String, Object>> apiResponse = APIResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Tài khoản của bạn đã bị khóa do vi phạm. Vui lòng thanh toán phí phạt để mở khóa.")
                .data(errorData)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
