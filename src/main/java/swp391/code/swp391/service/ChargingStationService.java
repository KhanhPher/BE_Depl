package swp391.code.swp391.service;

import swp391.code.swp391.dto.ChargingStationDTO;
import swp391.code.swp391.entity.ChargingStation.ChargingStationStatus;

import java.util.List;

public interface ChargingStationService {

    // Tạo charging station mới
    ChargingStationDTO createChargingStation(ChargingStationDTO chargingStationDTO);

    // Lấy charging station theo ID
    ChargingStationDTO getChargingStationById(Long stationId);

    // Lấy tất cả charging stations
    List<ChargingStationDTO> getAllChargingStations();

    // Cập nhật charging station
    ChargingStationDTO updateChargingStation(Long stationId, ChargingStationDTO chargingStationDTO);

    // Xóa charging station
    void deleteChargingStation(Long stationId);

    // Tìm kiếm charging stations theo tên
    List<ChargingStationDTO> searchChargingStationsByName(String stationName);

    // Tìm kiếm charging stations theo địa chỉ
    List<ChargingStationDTO> searchChargingStationsByAddress(String address);

    // Lấy charging stations theo status
    List<ChargingStationDTO> getChargingStationsByStatus(ChargingStationStatus status);

    // Cập nhật status của charging station
    ChargingStationDTO updateChargingStationStatus(Long stationId, ChargingStationStatus status);

    // Lấy stations có available charging points
    List<ChargingStationDTO> getStationsWithAvailableChargingPoints();

    // Lấy stations theo số lượng charging points tối thiểu
    List<ChargingStationDTO> getStationsWithMinimumChargingPoints(int minPoints);

    // Lấy stations không có charging points
    List<ChargingStationDTO> getStationsWithoutChargingPoints();

    // Lấy stations theo connector type
    List<ChargingStationDTO> getStationsByConnectorType(Long connectorTypeId);

    // Tìm kiếm stations theo địa chỉ và status
    List<ChargingStationDTO> searchStationsByAddressAndStatus(String address, ChargingStationStatus status);

    // Tìm stations gần vị trí (latitude, longitude)
    List<ChargingStationDTO> findNearbyStations(Double latitude, Double longitude, Double radiusKm);

    // Tìm kiếm stations theo tên và status
    List<ChargingStationDTO> searchStationsByNameAndStatus(String stationName, ChargingStationStatus status);

    // Đếm stations theo status
    long countStationsByStatus(ChargingStationStatus status);

    // Kiểm tra tên station đã tồn tại
    boolean isStationNameExists(String stationName);

    // Tìm stationId theo tên station
    Long findStationIdByName(String stationName);
}