package swp391.code.swp391.service;

import swp391.code.swp391.dto.StaffAssignmentDTO;
import swp391.code.swp391.dto.StaffDTO;

import java.util.List;

/**
 * Service interface for Staff Management at Charging Stations
 */
public interface StaffManagementService {

    /**
     * Get all staff members at a specific charging station
     * @param stationId ID of the charging station
     * @return List of staff members
     */
    List<StaffDTO> getStaffByStation(Long stationId);

    /**
     * Get all available staff (not assigned to any station or can be reassigned)
     * @return List of available staff members
     */
    List<StaffDTO> getAvailableStaff();

    /**
     * Assign a staff member to a charging station
     * @param assignmentDTO Assignment information
     * @return Updated staff information
     */
    StaffDTO assignStaffToStation(StaffAssignmentDTO assignmentDTO);

    /**
     * Remove a staff member from a charging station
     * @param userId ID of the staff member
     * @param stationId ID of the charging station
     * @return Confirmation message
     */
    String removeStaffFromStation(Long userId, Long stationId);

    /**
     * Update staff information
     * @param userId ID of the staff member
     * @param staffDTO Updated staff information
     * @return Updated staff information
     */
    StaffDTO updateStaff(Long userId, StaffDTO staffDTO);

    /**
     * Get staff details by ID
     * @param userId ID of the staff member
     * @return Staff information
     */
    StaffDTO getStaffById(Long userId);
}

