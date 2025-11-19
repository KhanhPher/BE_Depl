package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceFactorResponseDTO {
    private Long priceFactorId;
    private Long stationId;
    private Double factor;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
}

