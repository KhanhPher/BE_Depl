package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.IssueReport;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class IssueReportDTO {

    private Long issueReportId;
    private Long stationId;
    private String stationName;
    private Long staffId;
    private String staffName;
    private String description;
    private IssueReport.Status status;
    private LocalDateTime reportedTime;
}
