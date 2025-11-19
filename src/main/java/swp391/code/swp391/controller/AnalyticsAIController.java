package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.SuggestionDTO;
import swp391.code.swp391.service.AnalyticsAIService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai") //
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnalyticsAIController {

    // CHỈ TIÊM AnalyticsAIService
    private final AnalyticsAIService analyticsAIService;

    /**
     * API trả về danh sách gợi ý nên thêm/bớt trụ sạc
     * cho trang Dashboard của Admin (TÍNH NĂNG AI SỐ 2).
     */
    @GetMapping("/connector-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<APIResponse<List<SuggestionDTO>>> getConnectorSuggestions() {

        try {
            List<SuggestionDTO> suggestions = analyticsAIService.getConnectorSuggestions();

            String message = suggestions.isEmpty()
                    ? "Không có dữ liệu gợi ý"
                    : "Tải gợi ý thành công";

            APIResponse<List<SuggestionDTO>> response = APIResponse.<List<SuggestionDTO>>builder()
                    .success(true)
                    .message(message)
                    .data(suggestions)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting connector suggestions: {}", e.getMessage(), e);

            APIResponse<List<SuggestionDTO>> response = APIResponse.<List<SuggestionDTO>>builder()
                    .success(false)
                    .message("Lỗi khi lấy gợi ý trụ sạc: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}