package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho MỘT khoảng thời gian trống CÓ ĐỦ để sạc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTimeSlotDTO {

    // NEW: Fields used by OrderServiceImpl when creating slot DTOs
    private String slotId; // e.g. FIXED_02:00_04:00 or MINI_09:30_10:45
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private Integer slotDurationMinutes;
    private Double slotPrice;

    // Backward-compatible fields (kept for other usages)
    // Khoảng thời gian TRỐNG
    private LocalDateTime freeFrom; // Bắt đầu khoảng trống
    private LocalDateTime freeTo;   // Kết thúc khoảng trống
    private Integer availableMinutes; // Tổng số phút trống trong gap này

    // Thời gian cần thiết để sạc
    private Integer requiredMinutes; // Số phút CẦN để sạc đầy

    // Chi phí ước tính
    private Double estimatedCost; // VND (energyToCharge × pricePerKwh)
    private String slotType; // "FIXED" hoặc "MINI" - THÊM MỚI
}
