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
public class OrderResponseDTO {
    private Long orderId;

    // Thông tin trạm và điểm sạc
    private Long stationId;            // ID của trạm sạc
    private Long chargingPointId;      // ID của trụ sạc
    private String chargingPointName;  // Tên/số thứ tự của trụ sạc (VD: "A1", "A2", "B1")
    private String stationName;
    private String stationAddress;
    private String connectorType;

    // Thông tin user (driver)
    private Long userId;
    private String userName;           // user.fullName
    private String userPhone;          // user.phone

    // Thông tin vehicle
    private Long vehicleId;
    private String vehiclePlate;       // vehicle.plateNumber
    private String vehicleModel;       // vehicle.carModel.model
    // Thông tin battery
    private Double startedBattery;     // current battery
    private Double expectedBattery;    // target battery

    // Thông tin thời gian
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer estimatedDuration; // phút

    // Thông tin sạc
    private Double energyToCharge;     // kWh
    private Double chargingPower;      // kW
    private Double pricePerKwh;
    private Double estimatedCost;

    // Trạng thái
    private String status;

    // Trạng thái session (nếu có) - để FE biết session đang CHARGING/PARKING/COMPLETED
    private String sessionStatus;
    private Long sessionId; // ID của session (nếu có)

    // Thời gian tạo đơn
    private LocalDateTime createdAt;
}
