package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.IssueReportDTO;
import swp391.code.swp391.dto.IssueReportRequestDTO;
import swp391.code.swp391.dto.ParsedIssueReportDTO;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.IssueReport;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.ChargingStationRepository;
import swp391.code.swp391.repository.IssueReportRepository;
import swp391.code.swp391.repository.UserRepository;
import swp391.code.swp391.service.NotificationServiceImpl.IssueEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueReportServiceImpl implements IssueReportService {

    private final IssueReportRepository issueReportRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public Long createIssueReport(IssueReportRequestDTO dto, Long staffId) {
        // Validate station exists
        ChargingStation station = chargingStationRepository.findByStationId(dto.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // Validate staff exists
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        // Create issue report
        IssueReport issueReport = new IssueReport();
        issueReport.setStation(station);
        issueReport.setReporter(staff);
        issueReport.setDescription(dto.getDescription());
        issueReport.setStatus(IssueReport.Status.IN_PROGRESS);
        issueReport.setReportedTime(LocalDateTime.now());

        IssueReport saved = issueReportRepository.save(issueReport);

        // Send notification to admin
        notificationService.createIssueNotification(station.getStationId(), IssueEvent.STATION_ERROR_ADMIN, "New issue reported: " + dto.getDescription());

        // Send notification to staff reporter
        try {
            notificationService.createGeneralNotification(
                    List.of(staffId),
                    "Báo cáo sự cố đã được ghi nhận",
                    String.format("Bạn đã báo cáo cho quản trị viên vấn đề: %s tại trạm %s. Mã báo cáo: #%d",
                            dto.getDescription(),
                            station.getStationName(),
                            saved.getIssueReportId())
            );
        } catch (Exception e) {
            // Log but don't fail the transaction
            System.err.println("Lỗi khi tạo thông báo cho staff: " + e.getMessage());
        }

        return saved.getIssueReportId();
    }

    @Override
    public void updateStatusIssue(Long issueId, String status) {
        IssueReport issueReport = issueReportRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue report not found"));

        if (status.equalsIgnoreCase("RESOLVED")) {
            issueReport.setStatus(IssueReport.Status.RESOLVED);
        } else if (status.equalsIgnoreCase("IN_PROGRESS")) {
            issueReport.setStatus(IssueReport.Status.IN_PROGRESS);
        } else {
            throw new RuntimeException("Invalid status");
        }
        issueReportRepository.save(issueReport);

        notificationService.createIssueNotification(issueReport.getStation().getStationId(), IssueEvent.STATION_ERROR_STAFF, "Issue status updated! Status: " + issueReport.getStatus().name() + " " + issueReport.getDescription());
    }

    @Override
    public List<IssueReportDTO> getAllIssueReports() {
        List<IssueReport> issues = issueReportRepository.findAll();
        return issues.stream()
                .map(issue -> new IssueReportDTO(
                        issue.getIssueReportId(),
                        issue.getStation().getStationId(),
                        issue.getStation().getStationName(),
                        issue.getReporter().getUserId(),
                        issue.getReporter().getFullName(),
                        issue.getDescription(),
                        issue.getStatus(),
                        issue.getReportedTime()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Tạo một báo cáo sự cố mới từ dữ liệu đã được AI bóc tách.
     * Gộp các trường AI vào description.
     */
    @Override
    public IssueReport createReportFromParsedData(ParsedIssueReportDTO parsedReport, String originalFeedback) {

        // 1. Tìm trạm sạc (Entity) trong CSDL bằng tên
        String stationName = parsedReport.getStationName();
        if (stationName == null) {
            throw new RuntimeException("AI không thể bóc tách được tên trạm sạc.");
        }
        ChargingStation station = chargingStationRepository.findByStationName(stationName)
                .orElseThrow(() -> new RuntimeException("Trạm sạc '" + stationName + "' không tìm thấy trong CSDL."));

        // 2. TẠO NỘI DUNG MÔ TẢ (Theo yêu cầu của bạn)
        // Gộp tất cả thông tin AI và phản hồi gốc vào description
        String description = buildDescriptionFromAI(parsedReport, originalFeedback);

        // 3. Tạo đối tượng IssueReport mới
        IssueReport newReport = new IssueReport();

        newReport.setStation(station); // Gán trạm sạc
        newReport.setDescription(description); // Gán mô tả đã gộp

        newReport.setStatus(IssueReport.Status.IN_PROGRESS); // Đặt trạng thái ban đầu
        newReport.setReportedTime(LocalDateTime.now());

        // Lấy user hiện tại từ CSDL
        User currentUser = getCurrentUser();
        // Nếu không có user hiện tại, có thể để null hoặc gán user mặc định
        newReport.setReporter(currentUser); // Gán người báo cáo

        // 4. Lưu vào CSDL
        IssueReport savedReport = issueReportRepository.save(newReport);

        // 5. Gửi thông báo cho người dùng
        if (currentUser != null) {
            try {
                String issueTypeSummary = parsedReport.getIssueType() != null ? parsedReport.getIssueType() : "Sự cố chưa xác định";
                notificationService.createGeneralNotification(
                        List.of(currentUser.getUserId()),
                        "Báo cáo sự cố đã được ghi nhận",
                        String.format("Bạn đã báo cáo cho quản trị viên vấn đề: %s tại trạm %s. Mã báo cáo: #%d. Chúng tôi sẽ xử lý sớm nhất có thể.",
                                issueTypeSummary,
                                station.getStationName(),
                                savedReport.getIssueReportId())
                );
            } catch (Exception e) {
                // Log but don't fail the transaction
                System.err.println("Lỗi khi tạo thông báo cho user: " + e.getMessage());
            }
        }

        return savedReport;
    }

    /**
     * Hàm helper để gộp thông tin AI vào một chuỗi mô tả
     */
    private String buildDescriptionFromAI(ParsedIssueReportDTO parsedReport, String originalFeedback) {
        StringBuilder sb = new StringBuilder();

        sb.append("Phản hồi gốc của khách hàng:\n");
        sb.append("-----------------------------\n");
        sb.append("\"").append(originalFeedback).append("\"\n\n");

        sb.append("Phân tích tự động (AI):\n");
        sb.append("-----------------------------\n");

        if (parsedReport.getIssueType() != null && !parsedReport.getIssueType().isBlank()) {
            sb.append("- Loại lỗi: ").append(parsedReport.getIssueType()).append("\n");
        }
        if (parsedReport.getConnectorType() != null && !parsedReport.getConnectorType().isBlank()) {
            sb.append("- Trụ sạc nghi ngờ: ").append(parsedReport.getConnectorType()).append("\n");
        }
        if (parsedReport.getUserSentiment() != null && !parsedReport.getUserSentiment().isBlank()) {
            sb.append("- Cảm xúc: ").append(parsedReport.getUserSentiment()).append("\n");
        }

        return sb.toString();
    }
    /**
     * Hàm helper mới: Lấy User entity từ Spring Security
     */
    private User getCurrentUser() {
        // Lấy thông tin xác thực (authentication)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Không có ai đăng nhập (ví dụ: test Postman không token)
            return null;
        }

        // Lấy email (hoặc username) từ token
        String userEmail = authentication.getName();

        // Tìm User trong CSDL bằng email
        return userRepository.findByEmail(userEmail).orElse(null);
    }
}
