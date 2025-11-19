package swp391.code.swp391.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning/removing staff to/from charging station
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffAssignmentDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Station ID is required")
    private Long stationId;
}

