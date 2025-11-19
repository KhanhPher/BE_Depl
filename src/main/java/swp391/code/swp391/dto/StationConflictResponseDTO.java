package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationConflictResponseDTO {
    private Long stationId;
    private String stationName;
    private String address;
    private Integer totalConflicts;
    private List<ConflictGroup> conflictGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictGroup {
        private Long chargingPointId;
        private String chargingPointName;
        private String connectorType;
        private List<ConflictingOrderDTO> orders;
        private Integer conflictCount;
    }
}