package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Analytics Dashboard Summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsDashboardDTO {

    // Summary statistics
    private Long totalSessions;
    private Double totalEnergy;
    private Double totalRevenue;
    private Integer activeUsers;
    private Integer activeStations;

    // Recent sessions
    private List<ChargingSessionDTO> recentSessions;

    // Peak hours analysis
    private List<PeakHourDTO> peakHours;

    // Usage trends
    private List<UsageTrendDTO> trends;

    // Data availability flag
    private Boolean hasData;
    private String message; // Message when no data available
}

