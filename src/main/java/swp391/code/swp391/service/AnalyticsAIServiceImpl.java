package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.ConnectorUtilizationProjection;

import swp391.code.swp391.dto.SuggestionDTO;
import swp391.code.swp391.repository.AnalyticsRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Class triển khai CHỈ DÀNH CHO CÁC TÍNH NĂNG AI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAIServiceImpl implements AnalyticsAIService {

    // CHỈ CẦN AnalyticsRepository
    private final AnalyticsRepository analyticsRepository;

    // Ngưỡng để đưa ra gợi ý (cho AI nghiệp vụ)
    private static final double HIGH_UTILIZATION_THRESHOLD = 0.8; // 80%
    private static final double LOW_UTILIZATION_THRESHOLD = 0.05; // 5%
    private static final double TOTAL_HOURS_IN_30_DAYS = 24.0 * 30.0; // 720 giờ

    @Override
    public List<SuggestionDTO> getConnectorSuggestions() {
        log.info("Bắt đầu phân tích hiệu suất trụ sạc...");
        List<ConnectorUtilizationProjection> stats = analyticsRepository.getConnectorUtilizationStats();
        List<SuggestionDTO> suggestions = new ArrayList<>();

        for (ConnectorUtilizationProjection stat : stats) {
            double totalHoursAvailable = stat.getTotalPointsOfType() * TOTAL_HOURS_IN_30_DAYS;
            double utilizationRate = 0.0;
            if (totalHoursAvailable > 0) {
                utilizationRate = stat.getTotalHoursUsed() / totalHoursAvailable;
            }

            String message = "Hoạt động bình thường";

            if (utilizationRate > HIGH_UTILIZATION_THRESHOLD) {
                message = String.format("NÊN THÊM: Trụ đang quá tải (sử dụng %.1f%%)", utilizationRate * 100);
            } else if (utilizationRate < LOW_UTILIZATION_THRESHOLD && stat.getTotalPointsOfType() > 1) {
                message = String.format("NÊN GỠ BỚT/THAY THẾ: Trụ ít được sử dụng (%.1f%%)", utilizationRate * 100);
            }

            suggestions.add(new SuggestionDTO(
                    stat.getStationName(),
                    stat.getTypeName(),
                    message,
                    utilizationRate
            ));
        }

        log.info("Phân tích hiệu suất hoàn tất, tìm thấy {} kết quả.", suggestions.size());
        return suggestions;
    }
}