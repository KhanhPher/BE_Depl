package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.ChargingPointDTO;
import swp391.code.swp391.dto.ChargingStationDTO;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.ChargingStation.ChargingStationStatus;
import swp391.code.swp391.repository.ChargingStationRepository;
import swp391.code.swp391.repository.ConnectorTypeRepository;
import swp391.code.swp391.repository.OrderRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargingStationServiceImpl implements ChargingStationService {

    private final ChargingStationRepository chargingStationRepository;
    private final ChargingPointService chargingPointService;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final OrderRepository orderRepository;

    @Override
    public ChargingStationDTO createChargingStation(ChargingStationDTO chargingStationDTO) {
        // Xác thực đầu vào
        if (chargingStationDTO.getChargingPoints() == null || chargingStationDTO.getChargingPoints().isEmpty()) {
            throw new RuntimeException("At least one charging point is required");
        }

        // Xác thực số trụ sạc khớp với số trụ sạc thực tế
        if (chargingStationDTO.getChargingPointNumber() != chargingStationDTO.getChargingPoints().size()) {
            throw new RuntimeException("Charging point number must match the number of charging points provided");
        }

        // Xác nhận mỗi trụ sạc có một loại connector type
        for (ChargingPointDTO chargingPoint : chargingStationDTO.getChargingPoints()) {
            if (chargingPoint.getTypeName() == null || chargingPoint.getTypeName().isEmpty()) {
                throw new RuntimeException("Connector type name is required for each charging point");
            }
            // Xác thực connector type có tồn tại
            if (!connectorTypeRepository.existsByTypeName(chargingPoint.getTypeName())) {
                throw new RuntimeException("Invalid connector type name: " + chargingPoint.getTypeName());
            }
        }

        // Kiểm tra station ID đã tồn tại (chỉ khi có stationId)
        if (chargingStationDTO.getStationId() != null && chargingStationRepository.existsByStationId(chargingStationDTO.getStationId())) {
            throw new RuntimeException("Charging station with ID " + chargingStationDTO.getStationId() + " already exists");
        }

        // Kiểm tra tên station đã tồn tại
        if (chargingStationRepository.existsByStationName(chargingStationDTO.getStationName())) {
            throw new RuntimeException("Charging station with name '" + chargingStationDTO.getStationName() + "' already exists");
        }

        // Tạo mới charging station
        ChargingStation chargingStation = convertToEntity(chargingStationDTO);
        chargingStation.setLatitude(chargingStationDTO.getLatitude());  // Thêm dòng này
        chargingStation.setLongitude(chargingStationDTO.getLongitude()); // Thêm dòng này
        
        
        ChargingStation savedChargingStation = chargingStationRepository.save(chargingStation);


        // Tạo trụ sạc
        List<ChargingPointDTO> savedChargingPoints = new ArrayList<>();
        for (ChargingPointDTO chargingPointDTO : chargingStationDTO.getChargingPoints()) {
            chargingPointDTO.setStationId(savedChargingStation.getStationId());
            ChargingPointDTO savedChargingPoint = chargingPointService.createChargingPoint(chargingPointDTO);
            savedChargingPoints.add(savedChargingPoint);
        }

        ChargingStationDTO resultDTO = convertToDTO(savedChargingStation);
        resultDTO.setChargingPoints(savedChargingPoints);
        return resultDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingStationDTO getChargingStationById(Long stationId) {
        ChargingStation chargingStation = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + stationId));
        return convertToDTO(chargingStation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getAllChargingStations() {
        return chargingStationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChargingStationDTO updateChargingStation(Long stationId, ChargingStationDTO chargingStationDTO) {
        ChargingStation existingStation = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + stationId));

        // Kiểm tra tên mới có trùng với station khác không
        /*if (!existingStation.getStationName().equals(chargingStationDTO.getStationName()) &&
                chargingStationRepository.existsByStationName(chargingStationDTO.getStationName())) {
            throw new RuntimeException("Charging station with name '" + chargingStationDTO.getStationName() + "' already exists");
        }*/

        // Cập nhật thông tin
        existingStation.setStationName(chargingStationDTO.getStationName());
        existingStation.setAddress(chargingStationDTO.getAddress());
        existingStation.setStatus(chargingStationDTO.getStatus());
        existingStation.setLatitude(chargingStationDTO.getLatitude());
        existingStation.setLongitude(chargingStationDTO.getLongitude());

        ChargingStation updatedStation = chargingStationRepository.save(existingStation);
        return convertToDTO(updatedStation);
    }

    @Override
    public void deleteChargingStation(Long stationId) {
        // Ensure station exists before deleting (avoid unused local variable)
        chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + stationId));

        chargingStationRepository.deleteById(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> searchChargingStationsByName(String stationName) {
        return chargingStationRepository.findByStationNameContainingIgnoreCase(stationName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> searchChargingStationsByAddress(String address) {
        return chargingStationRepository.findByAddressContainingIgnoreCase(address).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getChargingStationsByStatus(ChargingStationStatus status) {
        return chargingStationRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChargingStationDTO updateChargingStationStatus(Long stationId, ChargingStationStatus status) {
        ChargingStation chargingStation = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + stationId));

        chargingStation.setStatus(status);
        ChargingStation updatedStation = chargingStationRepository.save(chargingStation);
        return convertToDTO(updatedStation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getStationsWithAvailableChargingPoints() {
        return chargingStationRepository.findStationsWithAvailableChargingPoints().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getStationsWithMinimumChargingPoints(int minPoints) {
        return chargingStationRepository.findStationsWithMinimumChargingPoints(minPoints).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getStationsWithoutChargingPoints() {
        return chargingStationRepository.findStationsWithoutChargingPoints().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> getStationsByConnectorType(Long connectorTypeId) {
        return chargingStationRepository.findStationsByConnectorType(connectorTypeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> searchStationsByAddressAndStatus(String address, ChargingStationStatus status) {
        return chargingStationRepository.findByAddressContainingIgnoreCaseAndStatus(address, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationDTO> searchStationsByNameAndStatus(String stationName, ChargingStationStatus status) {
        return chargingStationRepository.findByStationNameContainingIgnoreCaseAndStatus(stationName, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countStationsByStatus(ChargingStationStatus status) {
        return chargingStationRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStationNameExists(String stationName) {
        return chargingStationRepository.existsByStationName(stationName);
    }

    @Override
    public Long findStationIdByName(String stationName) {
        return chargingStationRepository.findStationIdByName(stationName);
    }

    @Override
    public List<ChargingStationDTO> findNearbyStations(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null) {
            throw new RuntimeException("Latitude và Longitude không được để trống");
        }

        // Lấy tất cả stations
        List<ChargingStation> allStations = chargingStationRepository.findAll();

        // Tính khoảng cách và filter
        List<ChargingStationDTO> result = new ArrayList<>();

        for (ChargingStation station : allStations) {
            // Skip stations without valid coordinates
            if (station.getLatitude() == 0.0 && station.getLongitude() == 0.0) {
                continue;
            }

            // Tính khoảng cách bằng Haversine formula
            double distance = calculateDistance(
                latitude, longitude,
                station.getLatitude(), station.getLongitude()
            );

            // Filter by radius
            if (distance <= radiusKm) {
                ChargingStationDTO dto = convertToDTO(station);
                dto.setDistance(distance);
                result.add(dto);
            }
        }

        // Sort by distance and limit to 10
        result.sort(Comparator.comparingDouble(ChargingStationDTO::getDistance));

        return result.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Tính khoảng cách giữa 2 điểm dựa trên Haversine formula
     * @return khoảng cách tính bằng km
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính Trái Đất (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Khoảng cách tính bằng km
    }

    // Helper methods
    private ChargingStation convertToEntity(ChargingStationDTO chargingStationDTO) {
        ChargingStation chargingStation = new ChargingStation();
        chargingStation.setStationId(chargingStationDTO.getStationId());
        chargingStation.setStationName(chargingStationDTO.getStationName());
        chargingStation.setAddress(chargingStationDTO.getAddress());
        chargingStation.setStatus(chargingStationDTO.getStatus() != null ?
                chargingStationDTO.getStatus() : ChargingStationStatus.ACTIVE);

        // Thêm latitude và longitude
        chargingStation.setLatitude(chargingStationDTO.getLatitude());
        chargingStation.setLongitude(chargingStationDTO.getLongitude());
        chargingStation.setStaff_id(chargingStation.getStaff_id());
        return chargingStation;
    }

    private ChargingStationDTO convertToDTO(ChargingStation chargingStation) {
        ChargingStationDTO dto = new ChargingStationDTO();
        dto.setStationId(chargingStation.getStationId());
        dto.setStationName(chargingStation.getStationName());
        dto.setAddress(chargingStation.getAddress());
        dto.setStatus(chargingStation.getStatus());
        dto.setChargingPointNumber(chargingStation.getChargingPointNumber());
        // Thêm latitude và longitude
        dto.setLatitude(chargingStation.getLatitude());
        dto.setLongitude(chargingStation.getLongitude());

        // Nếu cần trả về thông tin staff, tạo StaffDTO riêng và map ở đây
        // dto.setStaffList(chargingStation.getStaff_id().stream()
        //     .map(staff -> new StaffDTO(...))
        //     .collect(Collectors.toList()));

        // Thêm danh sách trụ sạc
        try {
            List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsByStationId(chargingStation.getStationId());
            dto.setChargingPoints(chargingPoints);
        } catch (Exception e) {
            // Log lỗi nếu cần thiết, nhưng không làm gián đoạn quá trình chuyển đổi
            System.err.println("Error fetching charging points: " + e.getMessage());
        }

        // Thêm số lượng order của trạm
        try {
            long orderCount = orderRepository.findByChargingPoint_Station_StationId(chargingStation.getStationId()).size();
            dto.setOrderCount(orderCount);
        } catch (Exception e) {
            // Log lỗi nếu cần thiết, nhưng không làm gián đoạn quá trình chuyển đổi
            System.err.println("Error fetching order count: " + e.getMessage());
        }

        return dto;
    }
}

