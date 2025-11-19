package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.CarModelDTO;
import swp391.code.swp391.dto.VehicleRequestDTO;
import swp391.code.swp391.dto.VehicleResponseDTO;
import swp391.code.swp391.entity.CarModel;
import swp391.code.swp391.entity.ConnectorType;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.entity.Vehicle;
import swp391.code.swp391.repository.UserRepository;
import swp391.code.swp391.repository.VehicleRepository;
import swp391.code.swp391.repository.CarModelRepository;
import swp391.code.swp391.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CarModelRepository carModelRepository;
    private final OrderRepository orderRepository;

    @Override
    public VehicleResponseDTO createVehicle(VehicleRequestDTO vehicleDTO) {
        // Kiểm tra plate number đã tồn tại
        if (vehicleRepository.existsByPlateNumber(vehicleDTO.getPlateNumber())) {
            throw new RuntimeException("Vehicle with plate number " + vehicleDTO.getPlateNumber() + " already exists");
        }

        Vehicle vehicle = convertToEntity(vehicleDTO);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getVehicleByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with plate number: " + plateNumber));
        return convertToDTO(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getVehiclesByUserId(Long userId) {
        return vehicleRepository.findByUserUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleResponseDTO updateVehicle(String plateNumber, VehicleRequestDTO vehicleDTO, Long userId) {
        Vehicle existingVehicle = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Kiểm tra vehicle có thuộc về user không
        if (existingVehicle.getUser() == null || !existingVehicle.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this vehicle");
        }

        // Kiểm tra vehicle có đang được đặt trong order không
        if (orderRepository.isVehicleCurrentlyBooked(existingVehicle.getId())) {
            throw new RuntimeException("Cannot update vehicle. This vehicle is currently booked in an active order.");
        }

        existingVehicle.setPlateNumber(vehicleDTO.getPlateNumber());


        // Cập nhật carModel nếu có
        if (vehicleDTO.getCarModelId() != null) {
            CarModel carModel = carModelRepository.findById(vehicleDTO.getCarModelId())
                    .orElseThrow(() -> new RuntimeException(
                            "CarModel not found with id: " + vehicleDTO.getCarModelId()));
            existingVehicle.setCarModel(carModel);
        }

        // Cập nhật user nếu có
        if (vehicleDTO.getUserId() != null) {
            User user = userRepository.findById(vehicleDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + vehicleDTO.getUserId()));
            existingVehicle.setUser(user);
        }

        Vehicle updatedVehicle = vehicleRepository.save(existingVehicle);
        return convertToDTO(updatedVehicle);
    }

    @Override
    public void deleteVehicleByUser(String plateNumber, Long userId) {
        Vehicle vehicle = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Kiểm tra vehicle có thuộc về user không
        if (vehicle.getUser() == null || !vehicle.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this vehicle");
        }

        // Kiểm tra vehicle có đang được đặt trong order không
        if (orderRepository.isVehicleCurrentlyBooked(vehicle.getId())) {
            throw new RuntimeException("Cannot delete vehicle. This vehicle is currently booked in an active order.");
        }

        vehicleRepository.deleteByPlateNumber(plateNumber);
    }


    // Helper methods
    private Vehicle convertToEntity(VehicleRequestDTO requestDTO) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(requestDTO.getPlateNumber());

        // Set carModel
        if (requestDTO.getCarModelId() != null) {
            CarModel carModel = carModelRepository.findById(requestDTO.getCarModelId())
                    .orElseThrow(() -> new RuntimeException(
                            "CarModel not found with id: " + requestDTO.getCarModelId()));
            vehicle.setCarModel(carModel);
        }

        // Set user
        if (requestDTO.getUserId() != null) {
            User user = userRepository.findById(requestDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + requestDTO.getUserId()));
            vehicle.setUser(user);
        }

        return vehicle;
    }

    private VehicleResponseDTO convertToDTO(Vehicle vehicle) {
        VehicleResponseDTO dto = new VehicleResponseDTO();
        dto.setVehicleId(vehicle.getId());
        dto.setPlateNumber(vehicle.getPlateNumber());
        dto.setVehicleId(vehicle.getId());
        if (vehicle.getCarModel() != null) {
            CarModel carModel = vehicle.getCarModel();

            List<Long> connectorTypeIds = null;
            if (carModel.getConnectorTypes() != null) {
                connectorTypeIds = carModel.getConnectorTypes().stream()
                        .map(ConnectorType::getConnectorTypeId)
                        .collect(Collectors.toList());
            }
            CarModelDTO carModelDTO = new CarModelDTO(
                    carModel.getCar_model_id(),
                    carModel.getBrand(),
                    carModel.getModel(),
                    carModel.getCapacity(),
                    carModel.getProductYear(),
                    connectorTypeIds,
                    carModel.getImg_url()

            );
            dto.setCarModel(carModelDTO);
        }

        // Cho response: set full objects
        if (vehicle.getCarModel() != null && vehicle.getUser() != null)
            dto.setUserId(vehicle.getUser().getUserId());

        return dto;
    }
}

