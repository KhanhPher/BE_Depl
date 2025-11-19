package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.ChargingPointDTO;
import swp391.code.swp391.dto.SessionDTO;
import swp391.code.swp391.entity.ChargingPoint.ChargingPointStatus;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.service.ChargingPointService;

import java.util.List;

@RestController
@RequestMapping("/api/charging-points")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChargingPointController {

    private final ChargingPointService chargingPointService;

    // Tạo charging point mới
    @PostMapping
    public ResponseEntity<?> createChargingPoint(@RequestBody @Validated ChargingPointDTO chargingPointDTO) {
        try {
            ChargingPointDTO createdChargingPoint = chargingPointService.createChargingPoint(chargingPointDTO);
            return new ResponseEntity<>(createdChargingPoint, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Lấy charging point theo ID
    @GetMapping("/{chargingPointId}")
    public ResponseEntity<?> getChargingPointById(@PathVariable Long chargingPointId) {
        try {
            ChargingPointDTO chargingPoint = chargingPointService.getChargingPointById(chargingPointId);
            return new ResponseEntity<>(chargingPoint, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Lấy tất cả charging points
    @GetMapping
    public ResponseEntity<List<ChargingPointDTO>> getAllChargingPoints() {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getAllChargingPoints();
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Lấy charging points theo station ID
    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<ChargingPointDTO>> getChargingPointsByStationId(@PathVariable Long stationId) {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsByStationId(stationId);
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Cập nhật charging point
    @PutMapping("/{chargingPointId}")
    public ResponseEntity<?> updateChargingPoint(@PathVariable Long chargingPointId,
                                                 @RequestBody @Validated ChargingPointDTO chargingPointDTO) {
        try {
            ChargingPointDTO updatedChargingPoint = chargingPointService.updateChargingPoint(chargingPointId, chargingPointDTO);
            return new ResponseEntity<>(updatedChargingPoint, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Xóa charging point
    @DeleteMapping("/{chargingPointId}")
    public ResponseEntity<?> deleteChargingPoint(@PathVariable Long chargingPointId) {
        try {
            chargingPointService.deleteChargingPoint(chargingPointId);
            return new ResponseEntity<>("Charging point deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Lấy charging points theo status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ChargingPointDTO>> getChargingPointsByStatus(@PathVariable ChargingPointStatus status) {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsByStatus(status);
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Lấy charging points theo station và status
    @GetMapping("/station/{stationId}/status/{status}")
    public ResponseEntity<List<ChargingPointDTO>> getChargingPointsByStationAndStatus(
            @PathVariable Long stationId,
            @PathVariable ChargingPointStatus status) {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsByStationAndStatus(stationId, status);
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Cập nhật status của charging point
    @PatchMapping("/{chargingPointId}/status")
    public ResponseEntity<?> updateChargingPointStatus(@PathVariable Long chargingPointId,
                                                       @RequestBody ChargingPointStatus status) {
        try {
            ChargingPointDTO updatedChargingPoint = chargingPointService.updateChargingPointStatus(chargingPointId, status);
            return new ResponseEntity<>(updatedChargingPoint, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Lấy available charging points có connector
    @GetMapping("/available-with-connectors")
    public ResponseEntity<List<ChargingPointDTO>> getAvailableChargingPointsWithConnectors() {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getAvailableChargingPointsWithConnectors();
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Lấy charging points theo connector type
    @GetMapping("/connector-type/{connectorTypeId}")
    public ResponseEntity<List<ChargingPointDTO>> getChargingPointsByConnectorType(@PathVariable Long connectorTypeId) {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsByConnectorType(connectorTypeId);
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Lấy charging points không có connectors
    @GetMapping("/without-connectors")
    public ResponseEntity<List<ChargingPointDTO>> getChargingPointsWithoutConnectors() {
        List<ChargingPointDTO> chargingPoints = chargingPointService.getChargingPointsWithoutConnectors();
        return new ResponseEntity<>(chargingPoints, HttpStatus.OK);
    }

    // Đếm charging points theo status
    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countChargingPointsByStatus(@PathVariable ChargingPointStatus status) {
        long count = chargingPointService.countChargingPointsByStatus(status);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    // Đếm charging points theo station
    @GetMapping("/count/station/{stationId}")
    public ResponseEntity<Long> countChargingPointsByStation(@PathVariable Long stationId) {
        long count = chargingPointService.countChargingPointsByStation(stationId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}