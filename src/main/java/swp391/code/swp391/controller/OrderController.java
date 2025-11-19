package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.service.CarModelServiceImpl;
import swp391.code.swp391.service.ChargingStationServiceImpl;
import swp391.code.swp391.service.OrderServiceImpl;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;
    private final ChargingStationServiceImpl chargingStationServiceImpl;
    private final CarModelServiceImpl carModelServiceImpl;

    /**
     * API 1: Find available slots - Tìm các khung giờ trống có đủ thời gian sạc
     */
    @PostMapping("/find-available-slots")
    public ResponseEntity<APIResponse<AvailableSlotsResponseDTO>> findAvailableSlots(
            @Valid @RequestBody OrderRequestDTO request) {

        AvailableSlotsResponseDTO response = orderServiceImpl.findAvailableSlots(request);

        int totalSlots = response.getChargingPoints().stream()
                .mapToInt(cp -> cp.getAvailableSlots().size())
                .sum();

        return ResponseEntity.ok(
                APIResponse.<AvailableSlotsResponseDTO>builder()
                        .success(true)
                        .message("Tìm thấy " + totalSlots + " khung giờ khả dụng")
                        .data(response)
                        .build()
        );
    }

    /**
     * API 2: Confirm order - Xác nhận đặt chỗ
     */
    @PostMapping("/confirm")
    public ResponseEntity<APIResponse<OrderResponseDTO>> confirmBooking(
            @Valid @RequestBody ConfirmOrderDTO request) {

        OrderResponseDTO order = orderServiceImpl.confirmOrder(request);

        return ResponseEntity.ok(
                APIResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Đặt chỗ thành công! Mã đơn: " + order.getOrderId())
                        .data(order)
                        .build()
        );
    }

    /**
     * API 3: Get fake battery level - Giả lập lấy % pin hiện tại
     */
    @GetMapping("/fake-battery/{vehicleId}")
    public ResponseEntity<APIResponse<BatteryLevelDTO>> getFakeBatteryLevel(
            @PathVariable Integer vehicleId) {

        // Giả lập random battery level từ 10% - 90%
        double batteryLevel = 10 + (Math.random() * 80);
        batteryLevel = Math.round(batteryLevel * 10.0) / 10.0;

        BatteryLevelDTO dto = BatteryLevelDTO.builder()
                .vehicleId(vehicleId)
                .currentBatteryPercent(batteryLevel)
                .batteryStatus(getBatteryStatus(batteryLevel))
                .needsChargingSoon(batteryLevel < 20)
                .build();

        return ResponseEntity.ok(
                APIResponse.<BatteryLevelDTO>builder()
                        .success(true)
                        .message("Lấy thông tin pin thành công")
                        .data(dto)
                        .build()
        );
    }

    /**
     * API 4: Get user's orders - Lấy danh sách đơn đặt chỗ của người dùng
     */
    @GetMapping("/my-orders")
    public ResponseEntity<APIResponse<List<OrderResponseDTO>>> getMyOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) Order.Status status) {

        List<OrderResponseDTO> orders = orderServiceImpl.getUserOrders(userId, status);

        return ResponseEntity.ok(
                APIResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Tìm thấy " + orders.size() + " đơn đặt chỗ")
                        .data(orders)
                        .build()
        );
    }

    /**
     * API 5: HỦY ĐƠN ĐẶT CHỖ
     */
    @PutMapping("/cancel")
    public ResponseEntity<APIResponse<OrderResponseDTO>> cancelOrder(
            @Valid @RequestBody CancelOrderDTO request) {

        OrderResponseDTO canceledOrder = orderServiceImpl.cancelOrder(request);

        return ResponseEntity.ok(
                APIResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Đã hủy đơn đặt chỗ thành công")
                        .data(canceledOrder)
                        .build()
        );
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<APIResponse<List<OrderResponseDTO>>> getStationOrders(
            @PathVariable Long stationId) {
        List<OrderResponseDTO> orders = orderServiceImpl.getStationOrders(stationId);
        return ResponseEntity.ok(
                APIResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Tìm thấy " + orders.size() + " đơn đặt chỗ")
                        .data(orders)
                        .build()
        );
    }

    /**
     * API 6: Check if vehicle is currently charging
     */
    @GetMapping("/vehicle/{vehicleId}/is-charging")
    public ResponseEntity<APIResponse<VehicleChargingStatusDTO>> checkVehicleChargingStatus(
            @PathVariable Long vehicleId) {

        boolean isCharging = orderServiceImpl.isVehicleCurrentlyCharging(vehicleId);

        VehicleChargingStatusDTO status = VehicleChargingStatusDTO.builder()
                .vehicleId(vehicleId)
                .isCurrentlyCharging(isCharging)
                .canBookNow(!isCharging)
                .message(isCharging
                    ? "Xe đang trong phiên sạc. Chỉ có thể đặt lịch (Schedule)."
                    : "Xe không đang sạc. Có thể Book Now hoặc Schedule.")
                .build();

        return ResponseEntity.ok(
                APIResponse.<VehicleChargingStatusDTO>builder()
                        .success(true)
                        .message("Lấy trạng thái xe thành công")
                        .data(status)
                        .build()
        );
    }


    // Helper method
    private String getBatteryStatus(double batteryLevel) {
        if (batteryLevel < 20) return "LOW";
        if (batteryLevel < 50) return "MEDIUM";
        if (batteryLevel < 80) return "GOOD";
        return "EXCELLENT";
    }

}
