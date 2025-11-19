package swp391.code.swp391.service;

import swp391.code.swp391.dto.SuggestionDTO;
import java.util.List;

/**
 * Service interface CHỈ DÀNH CHO CÁC TÍNH NĂNG AI
 * 1. AI Python: Trích xuất CSV
 * 2. AI Rule-Based: Gợi ý trụ sạc
 */
public interface AnalyticsAIService {

    /**
     * AI Feature 2: Phân tích và đưa ra gợi ý thêm/bớt trụ sạc.
     */
    List<SuggestionDTO> getConnectorSuggestions();
}