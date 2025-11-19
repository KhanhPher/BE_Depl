package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String avatarUrl;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private User.UserRole role;
    private User.UserStatus status;
    private int violations;

    // Station info for staff
    private Long stationId;
    private String stationName;

    // Danh sách xe của user
    private List<VehicleResponseDTO> vehicles;

    // Constructor từ User entity
    public UserDTO(User user) {
        this.userId = user.getUserId();
        this.username = user.getFullName();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhone();
        this.address = user.getAddress();
        this.role = user.getRole();
        this.dateOfBirth = user.getDateOfBirth();
        this.status = user.getStatus();
        this.avatarUrl = user.getAvatar();
        this.violations = user.getViolations();
        
        // Kiểm tra null cho station (chỉ STAFF mới có station)
        if (user.getStation() != null) {
            this.stationId = user.getStation().getStationId();
            this.stationName = user.getStation().getStationName();
        }
    }
    // Constructor từ User entity (có bao gồm vehicles)
    public UserDTO(User user, boolean includeVehicles) {
        this(user); // Gọi constructor trên

        if (includeVehicles && user.getVehicles() != null) {
            this.vehicles = user.getVehicles().stream()
                    .map(VehicleResponseDTO::new)
                    .collect(Collectors.toList());
        }
    }

    public boolean isBanned() {
        return this.status == User.UserStatus.BANNED;
    }
}