package swp391.code.swp391.dto;

import lombok.Data;

/**
 * DTO này là Input cho API /api/reports/submit-smart.
 * Nó chỉ chứa 1 câu text mà user nhập vào.
 */
@Data
public class SmartReportRequest {

    /**
     * Nội dung phản hồi, phàn nàn của người dùng.
     * Ví dụ: "Trụ CCS2 ở Vincom A sạc không vào điện!"
     */
    private String userFeedback;
    /**
     * ID của người dùng gửi phản hồi (nếu có).
     */    private Long userId;
}