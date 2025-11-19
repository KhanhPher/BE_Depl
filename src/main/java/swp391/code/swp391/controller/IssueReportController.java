package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.IssueReportDTO;
import swp391.code.swp391.dto.IssueReportRequestDTO;
import swp391.code.swp391.service.IssueReportService;
import swp391.code.swp391.util.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("/api/issue-reports")
@RequiredArgsConstructor
public class IssueReportController {

    private final IssueReportService issueReportService;
    private final JwtUtil jwtUtil;

    @PostMapping()
    public ResponseEntity<APIResponse<Long>> createIssueReport(@RequestBody IssueReportRequestDTO issueReportRequestDTO) {

        try {
            Long staffId = jwtUtil.getUserByTokenThroughSecurityContext().getUserId();
            Long issueId = issueReportService.createIssueReport(issueReportRequestDTO, staffId);
            return ResponseEntity.ok(APIResponse.success("Issue report created successfully", issueId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.error("Failed to create issue report: " + e.getMessage()));
        }
    }

    @PutMapping("/{issueId}/{status}")
    public ResponseEntity<APIResponse<Void>> updateStatusIssue(@PathVariable Long issueId, @PathVariable String status) {
        try {
            issueReportService.updateStatusIssue(issueId, status);
            return ResponseEntity.ok(APIResponse.success("Issue status updated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.error("Failed to update issue status: " + e.getMessage()));
        }
    }


    @GetMapping()
    public ResponseEntity<APIResponse<List<IssueReportDTO>>> getAllIssueReports() {
        try {
            List<IssueReportDTO> issues = issueReportService.getAllIssueReports();
            return ResponseEntity.ok(APIResponse.success("Issue reports retrieved successfully", issues));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.error("Failed to retrieve issue reports: " + e.getMessage()));
        }
    }
}
