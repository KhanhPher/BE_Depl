package swp391.code.swp391.service;

import swp391.code.swp391.dto.IssueReportDTO;
import swp391.code.swp391.dto.IssueReportRequestDTO;
import swp391.code.swp391.dto.ParsedIssueReportDTO;
import swp391.code.swp391.entity.IssueReport;

import java.util.List;

public interface IssueReportService {

    Long createIssueReport(IssueReportRequestDTO dto, Long staffId);

    void updateStatusIssue(Long issueId, String status);

    List<IssueReportDTO> getAllIssueReports();
    /**
     * Tạo một báo cáo sự cố mới từ dữ liệu đã được AI bóc tách.
     * Đây là hàm được gọi bởi Chatbot và SmartReportController.
     *
     * @param parsedReport DTO chứa dữ liệu Gemini bóc tách (tên trạm, loại lỗi...)
     * @param originalFeedback Câu văn phàn nàn gốc của người dùng
     * @return Entity IssueReport đã được lưu
     * @throws RuntimeException nếu không tìm thấy tên trạm sạc
     */
    IssueReport createReportFromParsedData(ParsedIssueReportDTO parsedReport, String originalFeedback);
}
