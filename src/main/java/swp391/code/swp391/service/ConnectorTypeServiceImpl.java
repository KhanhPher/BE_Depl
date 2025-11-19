package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.ConnectorTypeDTO;
import swp391.code.swp391.entity.ChargingPoint;
import swp391.code.swp391.entity.ConnectorType;
import swp391.code.swp391.repository.ChargingPointRepository;
import swp391.code.swp391.repository.ConnectorTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConnectorTypeServiceImpl implements ConnectorTypeService {

    private final ConnectorTypeRepository connectorTypeRepository;
    private final ChargingPointRepository chargingPointRepository;

    @Override
    public ConnectorTypeDTO createConnectorType(ConnectorTypeDTO connectorTypeDTO) {
        // Kiểm tra tên đã tồn tại
        if (connectorTypeRepository.existsByTypeName(connectorTypeDTO.getTypeName())) {
            throw new RuntimeException("Connector type with name '" + connectorTypeDTO.getTypeName() + "' already exists");
        }

        ConnectorType connectorType = convertToEntity(connectorTypeDTO);
        ConnectorType savedConnectorType = connectorTypeRepository.save(connectorType);
        return convertToDTO(savedConnectorType);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectorTypeDTO getConnectorTypeById(Long connectorTypeId) {
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new RuntimeException("Connector type not found with id: " + connectorTypeId));
        return convertToDTO(connectorType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorTypeDTO> getAllConnectorTypes() {
        return connectorTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorTypeDTO> getConnectorTypesByChargingPointId(Long chargingPointId) {
        return connectorTypeRepository.findByChargingPointId(chargingPointId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ConnectorTypeDTO updateConnectorType(Long connectorTypeId, ConnectorTypeDTO connectorTypeDTO) {
        ConnectorType existingConnectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new RuntimeException("Connector type not found with id: " + connectorTypeId));

        // Kiểm tra tên mới có trùng với connector type khác không
        if (!existingConnectorType.getTypeName().equals(connectorTypeDTO.getTypeName()) &&
                connectorTypeRepository.existsByTypeName(connectorTypeDTO.getTypeName())) {
            throw new RuntimeException("Connector type with name '" + connectorTypeDTO.getTypeName() + "' already exists");
        }

        // Cập nhật thông tin cơ bản
        existingConnectorType.setTypeName(connectorTypeDTO.getTypeName());
        existingConnectorType.setPowerOutput(connectorTypeDTO.getPowerOutput());
        existingConnectorType.setPricePerKWh(connectorTypeDTO.getPricePerKwh());

        // Cập nhật danh sách chargingPoints nếu có
        if (connectorTypeDTO.getChargingPointIds() != null && !connectorTypeDTO.getChargingPointIds().isEmpty()) {
            List<ChargingPoint> chargingPoints = connectorTypeDTO.getChargingPointIds().stream()
                    .map(id -> chargingPointRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Charging point not found with id: " + id)))
                    .collect(Collectors.toList());

            existingConnectorType.setChargingPoints(chargingPoints);

            // Nếu muốn đồng bộ ngược lại:
            for (ChargingPoint cp : chargingPoints) {
                cp.setConnectorType(existingConnectorType); // đảm bảo 2 chiều mapping đúng
            }
        } else {
            existingConnectorType.setChargingPoints(null);
        }

        ConnectorType updatedConnectorType = connectorTypeRepository.save(existingConnectorType);
        return convertToDTO(updatedConnectorType);
    }

    @Override
    public void deleteConnectorType(Long connectorTypeId) {
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new RuntimeException("Connector type not found with id: " + connectorTypeId));

        // Kiểm tra có vehicles đang sử dụng connector type này không
        if (connectorType.getCarModels() != null && !connectorType.getCarModels().isEmpty()) {
            throw new RuntimeException("Cannot delete connector type. It is being used by " +
                    connectorType.getCarModels().size() + " CarModel(s)");
        }

        connectorTypeRepository.deleteById(connectorTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorTypeDTO> searchConnectorTypesByName(String typeName) {
        return connectorTypeRepository.findByTypeNameContainingIgnoreCase(typeName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorTypeDTO> getConnectorTypesByVehiclePlateNumber(String plateNumber) {
        return connectorTypeRepository.findByVehiclePlateNumber(plateNumber).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectorTypeDTO> getUnassignedConnectorTypes() {
        return connectorTypeRepository.findUnassignedConnectorTypes().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectorTypeNameExists(String typeName) {
        return connectorTypeRepository.existsByTypeName(typeName);
    }

    // Helper methods
    private ConnectorType convertToEntity(ConnectorTypeDTO connectorTypeDTO) {
        ConnectorType connectorType = new ConnectorType();
        connectorType.setConnectorTypeId(connectorTypeDTO.getConnectorTypeId());
        connectorType.setTypeName(connectorTypeDTO.getTypeName());
        connectorType.setPowerOutput(connectorTypeDTO.getPowerOutput());
        connectorType.setPricePerKWh(connectorTypeDTO.getPricePerKwh());

        // Set danh sách ChargingPoints nếu có
        if (connectorTypeDTO.getChargingPointIds() != null && !connectorTypeDTO.getChargingPointIds().isEmpty()) {
            List<ChargingPoint> chargingPoints = connectorTypeDTO.getChargingPointIds().stream()
                    .map(id -> chargingPointRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("ChargingPoint not found with id: " + id)))
                    .collect(Collectors.toList());

            connectorType.setChargingPoints(chargingPoints);
        }

        return connectorType;
    }


    private ConnectorTypeDTO convertToDTO(ConnectorType connectorType) {
        ConnectorTypeDTO dto = new ConnectorTypeDTO();
        dto.setConnectorTypeId(connectorType.getConnectorTypeId());
        dto.setTypeName(connectorType.getTypeName());
        dto.setPowerOutput(connectorType.getPowerOutput());
        dto.setPricePerKwh(connectorType.getPricePerKWh());

        // Set danh sách ID của chargingPoints
        if (connectorType.getChargingPoints() != null) {
            List<Long> chargingPointIds = connectorType.getChargingPoints().stream()
                    .map(ChargingPoint::getChargingPointId)
                    .collect(Collectors.toList());
            dto.setChargingPointIds(chargingPointIds);
        }

        dto.setCarModel(connectorType.getCarModels());
        return dto;
    }

}

