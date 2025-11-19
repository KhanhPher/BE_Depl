package swp391.code.swp391.service;

import swp391.code.swp391.dto.*;

import java.util.List;

public interface StaffService {

    /**
     * Đổi trụ sạc cho driver khi trụ hiện tại bị chiếm dụng
     */
    ChangeChargingPointResponseDTO changeChargingPointForDriver(ChangeChargingPointRequestDTO request);

    /**
     * Tìm trụ sạc thay thế cùng loại connector trong cùng station
     */
    List<ChargingPointDTO> findAlternativeChargingPoints(Long orderId, Long currentChargingPointId);

    /**
     * Xem các order bị conflict thời gian tại station mà staff quản lý
     */
    StationConflictResponseDTO getConflictingOrdersByStation(Long stationId);

    /**
     * Xem tất cả stations mà staff đang quản lý
     */
    List<Long> getStationsManagedByStaff(Long staffId);

    TransactionHistoryDTO processOnsitePayment(Long sessionId, Long staffId);
    List<ChargingPointDTO> getChargingPointsByStation(Long stationId);
    Object createImmediateSession(OrderRequestDTO request);
    List<SessionListDTO> getSessionsByStation(Long stationId);
    Long createIssueReport(IssueReportDTO dto, Long staffId);
    List<TransactionHistoryDTO> getTransactionHistoryByStation(Long stationId);

}