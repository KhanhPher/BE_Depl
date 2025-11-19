package swp391.code.swp391.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Valid
public class IssueReportRequestDTO {

    @NotNull(message = "Station ID cannot be null")
    private Long stationId;

    @NotNull(message = "Description cannot be null")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "Urgency level cannot be null")
    private String urgencyLevel;

    @NotNull(message = "Status cannot be null")
    private String status;
}
