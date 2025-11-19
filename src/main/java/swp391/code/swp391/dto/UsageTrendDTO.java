package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Usage Trends and Statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageTrendDTO {

    private LocalDate date;
    private String period; // DAILY, WEEKLY, MONTHLY
    private Long totalSessions;
    private Double totalEnergy; // kWh
    private Double totalRevenue; // VND
    private Double averageSessionDuration; // minutes
    private Double averageEnergyPerSession; // kWh
    private Integer uniqueUsers;
    private Integer activeStations;

    // Growth metrics
    private Double sessionGrowthRate; // % compared to previous period
    private Double revenueGrowthRate; // % compared to previous period
    private Double energyGrowthRate; // % compared to previous period

    // Top performers
    private String topStation; // Station with most sessions
    private String topUser; // User with most sessions
    private String mostUsedConnectorType;

    // Peak information
    private Integer peakHour; // Hour with most activity
    private String peakDayOfWeek; // Day with most activity
}