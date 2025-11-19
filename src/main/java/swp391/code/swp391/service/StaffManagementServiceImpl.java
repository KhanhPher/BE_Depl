package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.StaffAssignmentDTO;
import swp391.code.swp391.dto.StaffDTO;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.ChargingStationRepository;
import swp391.code.swp391.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Staff Management at Charging Stations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffManagementServiceImpl implements StaffManagementService {

    private final UserRepository userRepository;
    private final ChargingStationRepository chargingStationRepository;

    @Override
    public List<StaffDTO> getStaffByStation(Long stationId) {
        log.info("Getting staff list for station ID: {}", stationId);

        // Validate station exists
        ChargingStation station = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with ID: " + stationId));

        // Get all staff assigned to this station
        List<User> staffList = userRepository.findByStation_StationId(stationId);

        return staffList.stream()
                .map(staff -> convertToDTO(staff, station))
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffDTO> getAvailableStaff() {
        log.info("Getting available staff members");

        // Get staff with role STAFF and no station assigned or ACTIVE status
        List<User> availableStaff = userRepository.findByRoleAndStatus(
                User.UserRole.STAFF,
                User.UserStatus.ACTIVE
        );

        return availableStaff.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StaffDTO assignStaffToStation(StaffAssignmentDTO assignmentDTO) {
        log.info("Assigning staff ID: {} to station ID: {}",
                assignmentDTO.getUserId(), assignmentDTO.getStationId());

        // Validate user exists and is a staff member
        User staff = userRepository.findById(assignmentDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + assignmentDTO.getUserId()));

        if (staff.getRole() != User.UserRole.STAFF) {
            throw new RuntimeException("User is not a staff member. Only users with STAFF role can be assigned to stations.");
        }

        if (staff.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("Staff account is not active. Status: " + staff.getStatus());
        }

        // Validate station exists
        ChargingStation station = chargingStationRepository.findById(assignmentDTO.getStationId())
                .orElseThrow(() -> new RuntimeException("Charging station not found with ID: " + assignmentDTO.getStationId()));

        if (station.getStatus() == ChargingStation.ChargingStationStatus.INACTIVE) {
            throw new RuntimeException("Cannot assign staff to inactive charging station");
        }

        // Check if staff is already assigned to this station
        if (staff.getStation() != null && staff.getStation().getStationId().equals(assignmentDTO.getStationId())) {
            throw new RuntimeException("Staff is already assigned to this station");
        }

        // Assign staff to station
        staff.setStation(station);
        User updatedStaff = userRepository.save(staff);

        log.info("Successfully assigned staff ID: {} to station ID: {}",
                updatedStaff.getUserId(), station.getStationId());

        return convertToDTO(updatedStaff, station);
    }

    @Override
    @Transactional
    public String removeStaffFromStation(Long userId, Long stationId) {
        log.info("Removing staff ID: {} from station ID: {}", userId, stationId);

        // Validate user exists
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Validate station exists
        ChargingStation station = chargingStationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Charging station not found with ID: " + stationId));

        // Check if staff is assigned to this station
        if (staff.getStation() == null || !staff.getStation().getStationId().equals(stationId)) {
            throw new RuntimeException("Staff is not assigned to this station");
        }

        // Remove station assignment
        staff.setStation(null);
        userRepository.save(staff);

        log.info("Successfully removed staff ID: {} from station ID: {}", userId, stationId);

        return String.format("Staff %s has been successfully removed from station %s",
                staff.getFullName(), station.getStationName());
    }

    @Override
    @Transactional
    public StaffDTO updateStaff(Long userId, StaffDTO staffDTO) {
        log.info("Updating staff ID: {}", userId);

        // Validate user exists
        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (staff.getRole() != User.UserRole.STAFF) {
            throw new RuntimeException("User is not a staff member");
        }

        // Update basic information
        if (staffDTO.getFullName() != null && !staffDTO.getFullName().isBlank()) {
            staff.setFullName(staffDTO.getFullName());
        }

        if (staffDTO.getEmail() != null && !staffDTO.getEmail().isBlank()) {
            // Check if email is already used by another user
            userRepository.findByEmail(staffDTO.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getUserId().equals(userId)) {
                    throw new RuntimeException("Email is already in use by another user");
                }
            });
            staff.setEmail(staffDTO.getEmail());
        }

        if (staffDTO.getPhone() != null && !staffDTO.getPhone().isBlank()) {
            // Check if phone is already used by another user
            userRepository.findByPhone(staffDTO.getPhone()).ifPresent(existingUser -> {
                if (!existingUser.getUserId().equals(userId)) {
                    throw new RuntimeException("Phone number is already in use by another user");
                }
            });
            staff.setPhone(staffDTO.getPhone());
        }

        if (staffDTO.getAddress() != null) {
            staff.setAddress(staffDTO.getAddress());
        }

        if (staffDTO.getDateOfBirth() != null) {
            staff.setDateOfBirth(staffDTO.getDateOfBirth());
        }

        if (staffDTO.getStatus() != null) {
            staff.setStatus(staffDTO.getStatus());
        }

        // Update station assignment if provided
        if (staffDTO.getStationId() != null) {
            ChargingStation station = chargingStationRepository.findById(staffDTO.getStationId())
                    .orElseThrow(() -> new RuntimeException("Charging station not found with ID: " + staffDTO.getStationId()));
            staff.setStation(station);
        }

        User updatedStaff = userRepository.save(staff);

        log.info("Successfully updated staff ID: {}", userId);

        return convertToDTO(updatedStaff);
    }

    @Override
    public StaffDTO getStaffById(Long userId) {
        log.info("Getting staff details for ID: {}", userId);

        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (staff.getRole() != User.UserRole.STAFF) {
            throw new RuntimeException("User is not a staff member");
        }

        return convertToDTO(staff);
    }

    // Helper methods for DTO conversion

    private StaffDTO convertToDTO(User staff) {
        return convertToDTO(staff, staff.getStation());
    }

    private StaffDTO convertToDTO(User staff, ChargingStation station) {
        StaffDTO dto = StaffDTO.builder()
                .userId(staff.getUserId())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phone(staff.getPhone())
                .dateOfBirth(staff.getDateOfBirth())
                .address(staff.getAddress())
                .role(staff.getRole())
                .status(staff.getStatus())
                .build();

        // Add station information if assigned
        if (station != null) {
            dto.setStationId(station.getStationId());
            dto.setStationName(station.getStationName());
            dto.setStationAddress(station.getAddress());
        }

        return dto;
    }
}

