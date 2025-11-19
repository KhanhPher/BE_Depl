package swp391.code.swp391.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token) {
        blacklist.put(token, System.currentTimeMillis());
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    // Optional: method to clean expired tokens, but since tokens expire, and we don't have TTL, perhaps not needed
}
