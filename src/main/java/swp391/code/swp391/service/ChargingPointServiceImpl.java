package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.ChargingPointDTO;
import swp391.code.swp391.entity.ChargingPoint;
import swp391.code.swp391.entity.ChargingPoint.ChargingPointStatus;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.ConnectorType;
import swp391.code.swp391.repository.ChargingPointRepository;
import swp391.code.swp391.repository.ChargingStationRepository;
import swp391.code.swp391.repository.ConnectorTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargingPointServiceImpl implements ChargingPointService {

    private final ChargingPointRepository chargingPointRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final ConnectorTypeRepository connectorTypeRepository;

    @Override
    public ChargingPointDTO createChargingPoint(ChargingPointDTO chargingPointDTO) {
        // Kiểm tra charging point ID đã tồn tại
        if (chargingPointRepository.existsByChargingPointId(chargingPointDTO.getChargingPointId())) {
            throw new RuntimeException("Charging point with ID " + chargingPointDTO.getChargingPointId() + " already exists");
        }

        // Validate station exists
        ChargingStation station = chargingStationRepository.findById(chargingPointDTO.getStationId())
                .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + chargingPointDTO.getStationId()));

        // Validate and get connector type by name
        ConnectorType connectorType = connectorTypeRepository.findByTypeName(chargingPointDTO.getTypeName())
                .orElseThrow(() -> new RuntimeException("Connector type not found with name: " + chargingPointDTO.getTypeName()));

        // Create charging point entity
        ChargingPoint chargingPoint = convertToEntity(chargingPointDTO);
        chargingPoint.setStation(station);
        chargingPoint.setConnectorType(connectorType);

        // Set default status if not provided
        if (chargingPoint.getStatus() == null) {
            chargingPoint.setStatus(ChargingPointStatus.AVAILABLE);
        }

        // Save charging point
        ChargingPoint savedChargingPoint = chargingPointRepository.save(chargingPoint);

        // Tăng chargingPointNumber khi tạo charging point
        station.setChargingPointNumber(station.getChargingPointNumber() + 1);
        chargingStationRepository.save(station);

        // Convert to DTO with connector type information
        ChargingPointDTO resultDTO = convertToDTO(savedChargingPoint);
        resultDTO.setTypeName(connectorType.getTypeName());
        resultDTO.setPowerOutput(connectorType.getPowerOutput());
        resultDTO.setPricePerKwh(connectorType.getPricePerKWh());

        return resultDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingPointDTO getChargingPointById(Long chargingPointId) {
        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new RuntimeException("Charging point not found with id: " + chargingPointId));
        return convertToDTO(chargingPoint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getAllChargingPoints() {
        return chargingPointRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getChargingPointsByStationId(Long stationId) {
        return chargingPointRepository.findByStationStationId(stationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChargingPointDTO updateChargingPoint(Long chargingPointId, ChargingPointDTO chargingPointDTO) {
        ChargingPoint existingChargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new RuntimeException("Charging point not found with id: " + chargingPointId));

        // Cập nhật status
        existingChargingPoint.setStatus(chargingPointDTO.getStatus());

        // Update connector type by name
        if (chargingPointDTO.getTypeName() != null) {
            ConnectorType connectorType = connectorTypeRepository.findByTypeName(chargingPointDTO.getTypeName())
                    .orElseThrow(() -> new RuntimeException("Connector type not found with name: " + chargingPointDTO.getTypeName()));
            existingChargingPoint.setConnectorType(connectorType);
        }

        // Cập nhật station nếu có
        if (chargingPointDTO.getStationId() != null) {
            ChargingStation station = chargingStationRepository.findById(chargingPointDTO.getStationId())
                    .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + chargingPointDTO.getStationId()));
            existingChargingPoint.setStation(station);
        }

        ChargingPoint updatedChargingPoint = chargingPointRepository.save(existingChargingPoint);
        return convertToDTO(updatedChargingPoint);
    }

    @Override
    public void deleteChargingPoint(Long chargingPointId) {
        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new RuntimeException("Charging point not found with id: " + chargingPointId));

        // Nếu vẫn đang liên kết với một connector type, không cho xoá
        if (chargingPoint.getConnectorType() != null && chargingPoint.getStatus() != ChargingPointStatus.AVAILABLE) {
            throw new RuntimeException("Cannot delete charging point. It is still associated with a connector type.");
        } else {
            chargingPoint.setConnectorType(null); // Bỏ liên kết với connector type
            chargingPointRepository.save(chargingPoint);
        }

        // Giảm chargingPointNumber khi xóa
        ChargingStation station = chargingPoint.getStation();
        if (station != null) {
            int currentNumber = station.getChargingPointNumber();
            // Check số âm
            if (currentNumber > 0) {
                station.setChargingPointNumber(currentNumber - 1);
                chargingStationRepository.save(station);
            }
        }

        chargingPointRepository.deleteById(chargingPointId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getChargingPointsByStatus(ChargingPointStatus status) {
        return chargingPointRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getChargingPointsByStationAndStatus(Long stationId, ChargingPointStatus status) {
        return chargingPointRepository.findByStationStationIdAndStatus(stationId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChargingPointDTO updateChargingPointStatus(Long chargingPointId, ChargingPointStatus status) {
        ChargingPoint chargingPoint = chargingPointRepository.findById(chargingPointId)
                .orElseThrow(() -> new RuntimeException("Charging point not found with id: " + chargingPointId));

        chargingPoint.setStatus(status);
        ChargingPoint updatedChargingPoint = chargingPointRepository.save(chargingPoint);
        return convertToDTO(updatedChargingPoint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getAvailableChargingPointsWithConnectors() {
        return chargingPointRepository.findAvailableChargingPointsWithConnectors().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getChargingPointsByConnectorType(Long connectorTypeId) {
        return chargingPointRepository.findByConnectorTypeId(connectorTypeId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingPointDTO> getChargingPointsWithoutConnectors() {
        return chargingPointRepository.findChargingPointsWithoutConnectors().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countChargingPointsByStatus(ChargingPointStatus status) {
        return chargingPointRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countChargingPointsByStation(Long stationId) {
        return chargingPointRepository.countByStationStationId(stationId);
    }

    // Helper methods
    private ChargingPoint convertToEntity(ChargingPointDTO chargingPointDTO) {
        ChargingPoint chargingPoint = new ChargingPoint();
        chargingPoint.setChargingPointId(chargingPointDTO.getChargingPointId());
        chargingPoint.setChargingPointName(chargingPointDTO.getChargingPointName());
        chargingPoint.setStatus(
                chargingPointDTO.getStatus() != null ? chargingPointDTO.getStatus() : ChargingPointStatus.AVAILABLE
        );

        // Set station
        if (chargingPointDTO.getStationId() != null) {
            ChargingStation station = chargingStationRepository.findById(chargingPointDTO.getStationId())
                    .orElseThrow(() -> new RuntimeException("Charging station not found with id: " + chargingPointDTO.getStationId()));
            chargingPoint.setStation(station);
        }

        // Set connectorType
        if (chargingPointDTO.getConnectorTypeId() != null) {
            ConnectorType connectorType = connectorTypeRepository.findById(chargingPointDTO.getConnectorTypeId())
                    .orElseThrow(() -> new RuntimeException("Connector type not found with id: " + chargingPointDTO.getConnectorTypeId()));
            chargingPoint.setConnectorType(connectorType);
        }

        // Nếu có typeName thì ưu tiên tìm theo tên
        if (chargingPointDTO.getTypeName() != null) {
            ConnectorType connectorTypeByName = connectorTypeRepository.findByTypeName(chargingPointDTO.getTypeName())
                    .orElseThrow(() -> new RuntimeException("Connector type not found with name: " + chargingPointDTO.getTypeName()));
            chargingPoint.setConnectorType(connectorTypeByName);
        }

        return chargingPoint;
    }

    private ChargingPointDTO convertToDTO (ChargingPoint chargingPoint){
            ChargingPointDTO dto = new ChargingPointDTO();
            dto.setChargingPointId(chargingPoint.getChargingPointId());
            dto.setChargingPointName(chargingPoint.getChargingPointName());
            dto.setStatus(chargingPoint.getStatus());

            // Output: full object nếu cần
            dto.setStation(chargingPoint.getStation());
            dto.setConnectorType(chargingPoint.getConnectorType());

            // Input: ID
            if (chargingPoint.getStation() != null)
                dto.setStationId(chargingPoint.getStation().getStationId());

            if (chargingPoint.getConnectorType() != null)
                dto.setConnectorTypeId(chargingPoint.getConnectorType().getConnectorTypeId());

            return dto;
        }
    }