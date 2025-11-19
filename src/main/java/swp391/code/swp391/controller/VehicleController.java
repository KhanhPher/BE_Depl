package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.VehicleRequestDTO;
import swp391.code.swp391.dto.VehicleResponseDTO;
import swp391.code.swp391.service.VehicleService;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    // Tạo vehicle mới
    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody @Validated VehicleRequestDTO vehicleDTO) {
        try {
            VehicleResponseDTO createdVehicle = vehicleService.createVehicle(vehicleDTO);
            return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Lấy vehicle theo plate number
    @GetMapping("/{plateNumber}")
    public ResponseEntity<?> getVehicleByPlateNumber(@PathVariable String plateNumber) {
        try {
            VehicleResponseDTO vehicle = vehicleService.getVehicleByPlateNumber(plateNumber);
            return new ResponseEntity<>(vehicle, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Lấy tất cả vehicles
    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles() {
        List<VehicleResponseDTO> vehicles = vehicleService.getAllVehicles();
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }

    // Lấy vehicles theo user ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VehicleResponseDTO>> getVehiclesByUserId(@PathVariable Long userId) {
        List<VehicleResponseDTO> vehicles = vehicleService.getVehiclesByUserId(userId);
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }

    // Cập nhật vehicle
    @PutMapping("/{plateNumber}/user/{userId}")
    public ResponseEntity<?> updateVehicle(@PathVariable String plateNumber,
                                             @PathVariable Long userId,
                                           @RequestBody @Validated VehicleRequestDTO vehicleDTO) {
        try {
            VehicleResponseDTO updatedVehicle = vehicleService.updateVehicle(plateNumber, vehicleDTO, userId);
            return new ResponseEntity<>(updatedVehicle, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Xóa vehicle
    @DeleteMapping("/user/{userId}/vehicle/{plateNumber}")
    public ResponseEntity<?> deleteVehicleByUser(@PathVariable Long userId, @PathVariable String plateNumber) {
        try {
            vehicleService.deleteVehicleByUser(plateNumber, userId);
            return new ResponseEntity<>("Vehicle deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}