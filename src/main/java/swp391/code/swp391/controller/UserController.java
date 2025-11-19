package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import swp391.code.swp391.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.service.UserServiceImpl;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.UpdateUserDTO;
import swp391.code.swp391.service.VehicleServiceImpl;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserServiceImpl userServiceImpl;
    private final VehicleServiceImpl vehicleServiceImpl;

    /**
     * 1. XEM THÔNG TIN USER
     * GET /api/user/profile/{id}
     * GET /api/user/profile/{id}?include=vehicles
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<APIResponse<UserDTO>> viewUserProfile(
            @PathVariable Long id,
            @RequestParam(required = false) String include) {
        // Đã có trong constructor nhờ @RequiredArgsConstructor
        try {
            // TODO: Gọi service để lấy thông tin user
            User user = userServiceImpl.getUserById(id);
            UserDTO userDTO = new UserDTO(user);

            // Kiểm tra nếu cần include vehicles
            if (include != null && include.contains("vehicles")) {
                // Lấy user kèm vehicles
                userDTO = userServiceImpl.getUserByIdWithVehicles(id);
            }
            return ResponseEntity.ok(
                    APIResponse.success("Lấy thông tin user thành công", userDTO)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("Không tìm thấy user: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * 2. CẬP NHẬT THÔNG TIN (cần xác nhận)
     * PUT /api/user/profile/{id}
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<APIResponse<UserDTO>> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO updateDTO) {

        try {
            // Gọi service để cập nhật thông tin user
            User updatedUser = userServiceImpl.updateUserProfile(id, updateDTO);

            // Convert to DTO to avoid serialization issues
            UserDTO userDTO = new UserDTO(updatedUser);

            return ResponseEntity.ok(
                    APIResponse.success("Cập nhật thông tin thành công", userDTO)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("lỗi: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi khi cập nhật: " + e.getMessage()));
        }
    }

    /**
     * 2.1 CẬP NHẬT MẬT KHẨU
     * PUT /api/user/profile/password/{id}
     */
    @PutMapping("/profile/password/{id}")
    public ResponseEntity<APIResponse<UserDTO>> updateUserPassWord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO updateDTO) {

        try {
            // TODO: Gọi service để cập nhật thông tin user
            User updatedUser = userServiceImpl.changePassword(id, updateDTO);
            UserDTO userDTO = new UserDTO(updatedUser);
            return ResponseEntity.ok(
                    APIResponse.success("Cập nhật thông tin thành công", userDTO)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("lỗi: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi khi cập nhật: " + e.getMessage()));
        }
    }


    /**
     * 3. XÓA USER
     * DELETE /api/user/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<String>> deleteUser(@PathVariable Long id) {
        try {
            userServiceImpl.deleteUser(id);
            return ResponseEntity.ok(
                    APIResponse.success("User đã được xóa thành công", null)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("Không tìm thấy user: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi khi xóa user: " + e.getMessage()));
        }
    }

    /**
     * 4. REPORT VIOLATION USER
     * PUT /api/user/reportViolation
     */
    @PostMapping("/reportViolation")
    public ResponseEntity<APIResponse<UserDTO>> reportViolation(
            @RequestParam Long userId,
            @RequestParam String reason) {
        try {
            UserDTO updatedUser = userServiceImpl.reportViolation(userId, reason);
            String message = updatedUser.isBanned()
                    ? "User đã bị ban do vượt quá số lần vi phạm"
                    : "Đã ghi nhận vi phạm cho user";
            return ResponseEntity.ok(
                    APIResponse.success(message, updatedUser)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("Không tìm thấy user: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi khi báo cáo vi phạm: " + e.getMessage()));
        }
    }
    /**
     * 5. XEM DANH SÁCH VEHICLE CỦA USER
     * GET /api/users/{id}/vehicles
     */
    @GetMapping("/{id}/vehicles")
    public ResponseEntity<APIResponse<List<VehicleResponseDTO>>> getUserVehicles(@PathVariable Long id) {
        try {
            List<VehicleResponseDTO> vehicles = vehicleServiceImpl.getVehiclesByUserId(id);
            return ResponseEntity.ok(
                    APIResponse.success("Lấy danh sách vehicle thành công", vehicles)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("Không tìm thấy user hoặc vehicle: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/allDriver")
    public ResponseEntity<APIResponse<List<UserDTO>>> getAllDriver() {
        try{
            List<UserDTO> users = userServiceImpl.getAllDriver();
            return ResponseEntity.ok(
                    APIResponse.success("Lấy danh sách driver thành công", users)
            );
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error("Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
