package swp391.code.swp391.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * CACHE CONFIG CHO GEMINI API
 * - Giảm số lần gọi API trùng lặp
 * - Tiết kiệm chi phí
 * - Tăng tốc độ response
 */
@Configuration
@EnableCaching
public class GeminiCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "gemini-responses",      // Cache cho câu trả lời
                "station-suggestions",   // Cache cho gợi ý trạm
                "market-trends",         // Cache cho xu hướng thị trường
                "amenities",             // Cache cho tiện ích xung quanh
                "parking-monitor"        // Cache cho parking duration monitoring (TTL: 30s)
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)           // Tối đa 1000 entries
                .expireAfterWrite(30, TimeUnit.MINUTES)  // Hết hạn sau 30 phút
                .recordStats());             // Theo dõi hit rate

        return cacheManager;
    }
}

