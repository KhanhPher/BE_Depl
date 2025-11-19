package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.ConnectorTypeDTO;
import swp391.code.swp391.service.ConnectorTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/connector-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConnectorTypeController {

    private final ConnectorTypeService connectorTypeService;

    // Tạo connector type mới
    @PostMapping
    public ResponseEntity<?> createConnectorType(@RequestBody @Validated ConnectorTypeDTO connectorTypeDTO) {
        try {
            ConnectorTypeDTO createdConnectorType = connectorTypeService.createConnectorType(connectorTypeDTO);
            return new ResponseEntity<>(createdConnectorType, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Lấy connector type theo ID
    @GetMapping("/{connectorTypeId}")
    public ResponseEntity<?> getConnectorTypeById(@PathVariable Long connectorTypeId) {
        try {
            ConnectorTypeDTO connectorType = connectorTypeService.getConnectorTypeById(connectorTypeId);
            return new ResponseEntity<>(connectorType, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Lấy tất cả connector types
    @GetMapping
    public ResponseEntity<List<ConnectorTypeDTO>> getAllConnectorTypes() {
        List<ConnectorTypeDTO> connectorTypes = connectorTypeService.getAllConnectorTypes();
        return new ResponseEntity<>(connectorTypes, HttpStatus.OK);
    }

    // Lấy connector types theo charging point ID
    @GetMapping("/charging-point/{chargingPointId}")
    public ResponseEntity<List<ConnectorTypeDTO>> getConnectorTypesByChargingPointId(@PathVariable Long chargingPointId) {
        List<ConnectorTypeDTO> connectorTypes = connectorTypeService.getConnectorTypesByChargingPointId(chargingPointId);
        return new ResponseEntity<>(connectorTypes, HttpStatus.OK);
    }

    // Cập nhật connector type
    @PutMapping("/{connectorTypeId}")
    public ResponseEntity<?> updateConnectorType(@PathVariable Long connectorTypeId,
                                                 @RequestBody @Validated ConnectorTypeDTO connectorTypeDTO) {
        try {
            ConnectorTypeDTO updatedConnectorType = connectorTypeService.updateConnectorType(connectorTypeId, connectorTypeDTO);
            return new ResponseEntity<>(updatedConnectorType, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Xóa connector type
    @DeleteMapping("/{connectorTypeId}")
    public ResponseEntity<?> deleteConnectorType(@PathVariable Long connectorTypeId) {
        try {
            connectorTypeService.deleteConnectorType(connectorTypeId);
            return new ResponseEntity<>("Connector type deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Tìm kiếm connector types theo tên
    @GetMapping("/search")
    public ResponseEntity<List<ConnectorTypeDTO>> searchConnectorTypesByName(@RequestParam String name) {
        List<ConnectorTypeDTO> connectorTypes = connectorTypeService.searchConnectorTypesByName(name);
        return new ResponseEntity<>(connectorTypes, HttpStatus.OK);
    }

    // Lấy connector types theo vehicle plate number
    @GetMapping("/vehicle/{plateNumber}")
    public ResponseEntity<List<ConnectorTypeDTO>> getConnectorTypesByVehiclePlateNumber(@PathVariable String plateNumber) {
        List<ConnectorTypeDTO> connectorTypes = connectorTypeService.getConnectorTypesByVehiclePlateNumber(plateNumber);
        return new ResponseEntity<>(connectorTypes, HttpStatus.OK);
    }

    // Lấy connector types chưa được assign
    @GetMapping("/unassigned")
    public ResponseEntity<List<ConnectorTypeDTO>> getUnassignedConnectorTypes() {
        List<ConnectorTypeDTO> connectorTypes = connectorTypeService.getUnassignedConnectorTypes();
        return new ResponseEntity<>(connectorTypes, HttpStatus.OK);
    }

    // Kiểm tra tên connector type có tồn tại không
    @GetMapping("/exists")
    public ResponseEntity<Boolean> isConnectorTypeNameExists(@RequestParam String typeName) {
        boolean exists = connectorTypeService.isConnectorTypeNameExists(typeName);
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }
}