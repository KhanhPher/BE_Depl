package swp391.code.swp391.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.code.swp391.entity.SystemConfig;
import swp391.code.swp391.repository.SystemConfigRepository;

@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    // Tên key mà chúng ta sẽ dùng
    public static final String MARKET_TRENDS_KEY = "admin_market_trends";

    /**
     * Lấy giá trị của một config
     */
    public String getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue) // Lấy giá trị nếu tồn tại
                .orElse(null); // Trả về null nếu không tìm thấy
    }

    /**
     * Cập nhật (hoặc tạo mới) một config
     */
    public void updateConfig(String key, String value, String description) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(new SystemConfig()); // Tạo mới nếu chưa có

        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);

        systemConfigRepository.save(config);
    }
}