package swp391.code.swp391.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetTokenService {
    private static final int TOKEN_EXPIRY_MINUTES = 10;
    private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();

    private static class TokenData {
        String email;
        LocalDateTime expiresAt;


        TokenData(String email, LocalDateTime expiresAt) {
            this.email = email;
            this.expiresAt = expiresAt;
        }
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

    }
    public String createToken(String email) {
        String token = java.util.UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        tokenStore.put(token, new TokenData(email, expiresAt));
        return token;
    }
    public String validateTokenAndGetEmail(String token) {
        TokenData tokenData = tokenStore.get(token);
        if (tokenData == null || tokenData.isExpired()) {
            return null;
        }
        return tokenData.email;
    }

    public void deleteToken(String token) {
        tokenStore.remove(token);
    }
}
