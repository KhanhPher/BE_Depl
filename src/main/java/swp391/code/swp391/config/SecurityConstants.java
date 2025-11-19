package swp391.code.swp391.config;

/**
 * Security constants for public endpoints and excluded paths
 * Used across SecurityConfig and various filters to avoid duplication
 *
 * Note: PUBLIC_ENDPOINTS_EXCLUDED are paths that are both public (no auth required)
 * and excluded from authorization checks. In this setup, they are the same.
 */
public class SecurityConstants {

    // Public endpoints that don't require authentication and are excluded from authz filters
    public static final String[] PUBLIC_ENDPOINTS_EXCLUDED = {
        "/api/auth/**",
        "/api/otp/**",
        "/api/payment/**",
        "/api/transactions/**",
        "/api/admin/revenue/**",
        "/api/notifications/connection/ws/**",
            "/actuator/**"
    };


    // WebSocket paths excluded from JWT filters
    public static final String[] WEBSOCKET_EXCLUDED_PATHS = {
        "/api/notifications/connection/ws"
    };
}
