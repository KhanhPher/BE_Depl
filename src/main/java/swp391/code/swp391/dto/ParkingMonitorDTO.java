package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for monitoring parking duration
 * Used for polling-based real-time updates without WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingMonitorDTO {

    private Long sessionId;
    private Long orderId;
    private String status; // PARKING, COMPLETED

    // Parking time tracking
    private LocalDateTime parkingStartTime;
    private LocalDateTime currentTime;
    private Long totalParkingMinutes;       // Tổng thời gian đã đỗ (phút)
    private Long totalParkingSeconds;       // Tổng thời gian đã đỗ (giây) - cho countdown

    // Grace period tracking
    private Long gracePeriodMinutes;        // Grace period (default: 15 phút)
    private Long remainingGraceMinutes;     // Còn lại bao nhiêu phút miễn phí
    private Long remainingGraceSeconds;     // Còn lại bao nhiêu giây miễn phí
    private Boolean isGracePeriodExpired;   // Đã hết grace period chưa

    // Fee calculation (preview)
    private Long chargeableMinutes;         // Số phút sẽ tính phí (nếu confirm rời ngay)
    private Double estimatedParkingFee;     // Phí đỗ xe ước tính (VNĐ)
    private Double parkingRatePerMinute;    // Giá mỗi phút (VNĐ/phút)
    
    // Charging cost
    private Double chargingCost;            // Chi phí phiên sạc (VNĐ) - từ session.baseCost
    private Double powerConsumed;           // Năng lượng tiêu thụ (kWh) - từ session.powerConsumed

    // Warnings
    private String warningMessage;          // Cảnh báo cho user
    private String warningLevel;            // INFO, WARNING, DANGER

    // Station info
    private String stationName;
    private String chargingPointName;

    /**
     * Helper method to determine warning level
     */
    public static String calculateWarningLevel(long totalMinutes, long gracePeriodMinutes) {
        if (totalMinutes < gracePeriodMinutes * 0.7) {
            return "INFO"; // < 70% grace period
        } else if (totalMinutes < gracePeriodMinutes) {
            return "WARNING"; // 70-100% grace period
        } else {
            return "DANGER"; // > grace period - đang tính phí
        }
    }

    /**
     * Helper method to generate warning message
     */
    public static String generateWarningMessage(long totalMinutes, long remainingGraceMinutes, boolean isExpired) {
        if (!isExpired && remainingGraceMinutes > 5) {
            return String.format("Bạn còn %d phút miễn phí. Vui lòng xác nhận rời trạm khi sẵn sàng.", remainingGraceMinutes);
        } else if (!isExpired && remainingGraceMinutes > 0) {
            return String.format("CẢNH BÁO: Chỉ còn %d phút miễn phí! Vui lòng rời ngay.", remainingGraceMinutes);
        } else {
            long overtimeMinutes = totalMinutes - 15;
            return String.format("ĐÃ HẾT GRACE PERIOD! Phí đỗ xe đang được tính: %d phút", overtimeMinutes);
        }
    }
}

