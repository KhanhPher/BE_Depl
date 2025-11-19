package swp391.code.swp391.service;

import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.exception.ApiRequestException;

import java.util.List;

public interface OrderService {

    /**
     * Tìm các khung giờ sạc trống tại trạm cụ thể dựa trên yêu cầu của người dùng.
     */
    AvailableSlotsResponseDTO findAvailableSlots(OrderRequestDTO request);

    /**
     * Xác nhận đặt chỗ sạc, tạo mới một đơn đặt trong hệ thống.
     */
    OrderResponseDTO confirmOrder(ConfirmOrderDTO request);

    /**
     * Lấy danh sách các đơn sạc của người dùng.
     */
    List<OrderResponseDTO> getUserOrders(Long userId, Order.Status status);

    /**
     * Hủy đơn đặt chỗ.
     *
     * @param request Thông tin hủy đơn (orderId, userId, reason)
     * @return Order đã được hủy
     * @throws ApiRequestException nếu không thể hủy
     */
    OrderResponseDTO cancelOrder(CancelOrderDTO request);

    /**
     * Kiểm tra xe có đang trong phiên sạc (CHARGING) hay không.
     *
     * @param vehicleId ID của xe cần kiểm tra
     * @return true nếu xe đang sạc, false nếu không
     */
    boolean isVehicleCurrentlyCharging(Long vehicleId);
}