package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO cho danh sách slots của MỘT charging point
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingPointAvailabilityDTO {

    private Long chargingPointId;
    private String chargingPointName; // Số thứ tự/tên của charging point (VD: "A1", "A2", "B1")
    private String connectorTypeName;
    private Double chargingPower; // kW
    private Double pricePerKwh; // VND/kWh

    // Thời gian sạc cần thiết với connector này
    private Integer requiredMinutes;

    // Danh sách các khoảng thời gian TRỐNG có ĐỦ THỜI GIAN để sạc
    private List<AvailableTimeSlotDTO> availableSlots;

    // Tổng số phút trống CÓ THỂ SỬ DỤNG (chỉ tính gaps đủ thời gian)
    private Integer totalAvailableMinutes;
}
