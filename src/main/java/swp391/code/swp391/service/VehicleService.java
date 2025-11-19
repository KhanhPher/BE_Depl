
    package swp391.code.swp391.service;

    import swp391.code.swp391.dto.VehicleRequestDTO;
    import swp391.code.swp391.dto.VehicleResponseDTO;

    import java.util.List;

    public interface VehicleService {

        // Create a new vehicle (accept request DTO, return response DTO)
        VehicleResponseDTO createVehicle(VehicleRequestDTO vehicleDTO);

        // Get vehicle by plate number
        VehicleResponseDTO getVehicleByPlateNumber(String plateNumber);

        // Get all vehicles
        List<VehicleResponseDTO> getAllVehicles();

        // Get vehicles by user ID
        List<VehicleResponseDTO> getVehiclesByUserId(Long userId);

        // Update vehicle (accept request DTO, return response DTO)
        VehicleResponseDTO updateVehicle(String plateNumber, VehicleRequestDTO vehicleDTO, Long userId);

        // Delete vehicle with user validation
        void deleteVehicleByUser(String plateNumber, Long userId);

    }