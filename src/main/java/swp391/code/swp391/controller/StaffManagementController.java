package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.StaffAssignmentDTO;
import swp391.code.swp391.dto.StaffDTO;
import swp391.code.swp391.service.StaffManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/staff-management")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class StaffManagementController {

    private final StaffManagementService staffManagementService;

    @GetMapping("/stations/{stationId}/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<List<StaffDTO>>> getStaffByStation(
            @PathVariable Long stationId) {
        List<StaffDTO> staffList;
        try {
            staffList = staffManagementService.getStaffByStation(stationId);
        } catch (RuntimeException e) {
            log.error("Error getting staff list for station ID {}: {}", stationId, e.getMessage());
            return ResponseEntity.badRequest().body(APIResponse.<List<StaffDTO>>error("Error: " + e.getMessage()));
        }
        return ResponseEntity.ok(APIResponse.success("Get all staff members at station "+ stationId +" successful",staffList));
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<StaffDTO>>> getAvailableStaff() {
        List<StaffDTO> staffList;
        try {
            staffList = staffManagementService.getAvailableStaff();
        } catch (RuntimeException e) {
            log.error("Error getting available staff: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.<List<StaffDTO>>error("Error: " + e.getMessage()));
        }
        return ResponseEntity.ok(APIResponse.success("Get all available staff successful",staffList));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<StaffDTO>> assignStaffToStation(
            @Valid @RequestBody StaffAssignmentDTO assignmentDTO) {
        StaffDTO assignedStaff;
        try {
            assignedStaff = staffManagementService.assignStaffToStation(assignmentDTO);
        } catch (RuntimeException e) {
            log.error("Error assigning staff: {}", e.getMessage());
            return ResponseEntity.badRequest().body(APIResponse.error("Assignment failed: " + e.getMessage()));
        }
        return ResponseEntity.ok(APIResponse.success("Staff assigned successfully",assignedStaff));
    }


    @DeleteMapping("/stations/{stationId}/staff/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<String>> removeStaffFromStation(
            @PathVariable Long stationId,
            @PathVariable Long userId) {
        String message;
        try {
            message = staffManagementService.removeStaffFromStation(userId, stationId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseEntity.badRequest().body(APIResponse.<String>error("Removal failed: " + e.getMessage())).getBody());
        }
        return ResponseEntity.ok(APIResponse.success("Staff removed successfully",message));
    }

    @PutMapping("/staff/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<StaffDTO>> updateStaff(
            @PathVariable Long userId,
            @Valid @RequestBody StaffDTO staffDTO) {
        StaffDTO updatedStaff;
        try {
            updatedStaff = staffManagementService.updateStaff(userId, staffDTO);
        } catch (RuntimeException e) {
            log.error("Error updating staff ID {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(APIResponse.<StaffDTO>error("Update failed: " + e.getMessage()));
        }
        return ResponseEntity.ok(APIResponse.success("Staff updated successfully",updatedStaff));
    }

    @GetMapping("/staff/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<APIResponse<StaffDTO>> getStaffById(@PathVariable Long userId) {
        StaffDTO staff;
        try {
            staff = staffManagementService.getStaffById(userId);
        } catch (RuntimeException e) {
            log.error("Error getting staff ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error("Error: " + e.getMessage()));
        }
        return ResponseEntity.ok(APIResponse.success("Get staff by ID successful",staff));
    }
}

