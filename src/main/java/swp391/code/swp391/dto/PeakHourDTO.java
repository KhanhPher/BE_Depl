package swp391.code.swp391.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Peak Hours Analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeakHourDTO {

    private Integer hour; // 0-23
    private String timeRange; // e.g., "08:00 - 09:00"
    private Long sessionCount; // Number of sessions in this hour
    private Double totalEnergy; // Total kWh consumed
    private Double averageEnergy; // Average kWh per session
    private Double totalRevenue; // Total revenue in this hour
    private String peakLevel; // LOW, MEDIUM, HIGH, VERY_HIGH
    private Integer percentageOfDaily; // % of daily total
}

