
package swp391.code.swp391.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.service.SystemConfigService;

@RestController
@RequestMapping("/api/admin/widget")
@CrossOrigin(origins = "*")
public class AdminWidgetController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * API cho Admin Dashboard gọi để lấy nội dung widget
     */
    @GetMapping("/market-trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getMarketTrends() {

        // 1. Lấy tin tức đã được lưu (do ScheduledTask cập nhật)
        String trends = systemConfigService.getConfigValue(
                SystemConfigService.MARKET_TRENDS_KEY
        );

        if (trends == null) {
            return ResponseEntity.ok("Đang tải dữ liệu xu hướng...");
        }

        // 2. Trả về một chuỗi text (frontend sẽ hiển thị)
        return ResponseEntity.ok(trends);
    }
}