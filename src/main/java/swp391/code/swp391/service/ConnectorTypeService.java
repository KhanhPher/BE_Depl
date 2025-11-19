package swp391.code.swp391.service;

import swp391.code.swp391.dto.ConnectorTypeDTO;

import java.util.List;

public interface ConnectorTypeService {

    // Tạo connector type mới
    ConnectorTypeDTO createConnectorType(ConnectorTypeDTO connectorTypeDTO);

    // Lấy connector type theo ID
    ConnectorTypeDTO getConnectorTypeById(Long connectorTypeId);

    // Lấy tất cả connector types
    List<ConnectorTypeDTO> getAllConnectorTypes();

    // Lấy connector types theo charging point ID
    List<ConnectorTypeDTO> getConnectorTypesByChargingPointId(Long chargingPointId);

    // Cập nhật connector type
    ConnectorTypeDTO updateConnectorType(Long connectorTypeId, ConnectorTypeDTO connectorTypeDTO);

    // Xóa connector type
    void deleteConnectorType(Long connectorTypeId);

    // Tìm kiếm connector type theo tên
    List<ConnectorTypeDTO> searchConnectorTypesByName(String typeName);

    // Lấy connector types theo vehicle plate number
    List<ConnectorTypeDTO> getConnectorTypesByVehiclePlateNumber(String plateNumber);

    // Lấy connector types chưa được assign
    List<ConnectorTypeDTO> getUnassignedConnectorTypes();

    // Kiểm tra tên connector type đã tồn tại
    boolean isConnectorTypeNameExists(String typeName);
}