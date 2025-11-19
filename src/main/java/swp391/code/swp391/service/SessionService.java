package swp391.code.swp391.service;


import swp391.code.swp391.dto.SessionProgressDTO;
import swp391.code.swp391.dto.SessionDTO;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.Vehicle;

import java.util.List;

public interface SessionService {

    boolean isValidTime(Long orderId,int maxStartDelayMinutes);

    /**
     * Bắt đầu phiên sạc với kiểm tra khoảng cách
     * @param userId ID người dùng
     * @param orderId ID đơn đặt chỗ
     * @param vehicleId ID xe
     * @param userLatitude Vĩ độ hiện tại của người dùng
     * @param userLongitude Kinh độ hiện tại của người dùng
     * @return Session ID
     */
    Long startSession(Long userId, Long orderId, Long vehicleId, Double userLatitude, Double userLongitude);

    Long endSession(Long sessionId, Long userId);

    SessionProgressDTO monitorSession(Long sessionId, Long userId);

    Double calculatePenaltyAmount(String type, Order order);

    Double calculateBatteryPercentage(Vehicle vehicle, Double kwh);

    long expectedMinutes(Vehicle vehicle, Double expectedBattery);

    // --- New management APIs ---
    List<SessionDTO> getAllSessions();

    SessionDTO getSessionDetails(Long sessionId);

    SessionDTO getSessionByOrderId(Long orderId);

    Long forceEndSession(Long sessionId, Long operatorId); // For admin/operator use
}
