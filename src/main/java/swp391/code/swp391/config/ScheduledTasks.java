package swp391.code.swp391.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp391.code.swp391.service.GeminiService;
import swp391.code.swp391.service.SystemConfigService;

@Component
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * Tự động chạy vào 5:00 sáng HẰNG NGÀY.
     * (Cú pháp cron: Giây Phút Giờ Ngày Tháng Ngày-trong-tuần)
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void updateMarketTrendsWidget() {
        logger.info("Đang chạy tác vụ: Cập nhật xu hướng thị trường (Gemini)...");
        try {
            // 1. Gọi GeminiService (hàm này bạn đã có từ trước)
            String trends = geminiService.getMarketTrends();

            if (trends != null && !trends.isBlank()) {
                // 2. Lưu kết quả vào CSDL
                systemConfigService.updateConfig(
                        SystemConfigService.MARKET_TRENDS_KEY,
                        trends,
                        "Widget xu hướng thị trường cho admin"
                );
                logger.info("Cập nhật xu hướng thị trường thành công!");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật xu hướng thị trường", e);
        }
    }
}