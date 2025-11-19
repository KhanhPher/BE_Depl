package swp391.code.swp391.dto;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class BatteryLevelDTO {
    private Integer vehicleId;
    private Double currentBatteryPercent;
    private String batteryStatus;
    private Boolean needsChargingSoon;

}