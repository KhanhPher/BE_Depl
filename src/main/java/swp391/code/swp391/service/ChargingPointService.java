package swp391.code.swp391.service;

import swp391.code.swp391.dto.ChargingPointDTO;
import swp391.code.swp391.entity.ChargingPoint.ChargingPointStatus;

import java.util.List;

public interface ChargingPointService {

    // Tạo charging point mới
    ChargingPointDTO createChargingPoint(ChargingPointDTO chargingPointDTO);

    // Lấy charging point theo ID
    ChargingPointDTO getChargingPointById(Long chargingPointId);

    // Lấy tất cả charging points
    List<ChargingPointDTO> getAllChargingPoints();

    // Lấy charging points theo station ID
    List<ChargingPointDTO> getChargingPointsByStationId(Long stationId);

    // Cập nhật charging point
    ChargingPointDTO updateChargingPoint(Long chargingPointId, ChargingPointDTO chargingPointDTO);

    // Xóa charging point
    void deleteChargingPoint(Long chargingPointId);

    // Tìm kiếm charging points theo status
    List<ChargingPointDTO> getChargingPointsByStatus(ChargingPointStatus status);

    // Tìm charging points theo station và status
    List<ChargingPointDTO> getChargingPointsByStationAndStatus(Long stationId, ChargingPointStatus status);

    // Cập nhật status của charging point
    ChargingPointDTO updateChargingPointStatus(Long chargingPointId, ChargingPointStatus status);

    // Lấy available charging points có connector
    List<ChargingPointDTO> getAvailableChargingPointsWithConnectors();

    // Lấy charging points theo connector type
    List<ChargingPointDTO> getChargingPointsByConnectorType(Long connectorTypeId);

    // Lấy charging points không có connector types
    List<ChargingPointDTO> getChargingPointsWithoutConnectors();

    // Đếm charging points theo status
    long countChargingPointsByStatus(ChargingPointStatus status);

    // Đếm charging points theo station
    long countChargingPointsByStation(Long stationId);

    // US10
//    SessionDTO confirmCharging(Long orderId, Long vehicleId,Long connectorTypeId);
}