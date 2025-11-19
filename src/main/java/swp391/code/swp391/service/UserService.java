package swp391.code.swp391.service;
import swp391.code.swp391.dto.RegisterRequestDTO;
import swp391.code.swp391.dto.UserDTO;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.dto.UpdateUserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Long registerUser(RegisterRequestDTO registerDTO);
    boolean isValidEmail(String email);
    boolean isValidVietnamPhone(String phone);
    Long addUser(User user);
    boolean checkUserExistsByEmail(String email);
    boolean checkUserExistsByPhone(String phone);
    User getUserByPhone(String phone);
    /**
     * Lấy thông tin user theo ID.
     * @param id ID của user
     * @return User object
     * @throws RuntimeException nếu không tìm thấy user
     */
    User getUserById(Long id);

    /**
     * Lấy thông tin user theo ID và trả về UserDTO (có bao gồm vehicles)
     */
    UserDTO getUserByIdWithVehicles(Long id);

    /**
     * Lấy thông tin user theo email.
     * @param email Email của user
     * @return Optional chứa User object nếu tìm thấy, ngược lại Optional rỗng
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Kiểm tra xem email đã tồn tại trong hệ thống chưa.
     * @param email Email cần kiểm tra
     * @return true nếu email đã tồn tại, false nếu chưa tồn tại
     */
    boolean isEmailExists(String email);

    /**
     * Kiểm tra xem số điện thoại đã tồn tại trong hệ thống chưa.
     * @param phone Số điện thoại cần kiểm tra
     * @return true nếu số điện thoại đã tồn tại, false nếu chưa tồn tại
     */
    boolean isPhoneExists(String phone);

    /**
     * Cập nhật thông tin profile của user.
     * @param userId ID của user
     * @param updateDTO DTO chứa thông tin cần cập nhật
     * @return User object đã cập nhật
     * @throws RuntimeException nếu không tìm thấy user
     */
    User updateUserProfile(Long userId, UpdateUserDTO updateDTO);


    /**
     * Gửi mã xác thực đến số điện thoại mới.
     * @param userId ID của user
     * @param newPhone Số điện thoại mới cần xác thực
     * @return String thông báo kết quả
     * @throws RuntimeException nếu không tìm thấy user
     */
    String sendPhoneVerification(Long userId, String newPhone);

    /**
     * Xác thực và cập nhật số điện thoại mới.
     * @param userId ID của user
     * @param newPhone Số điện thoại mới
     * @param verificationCode Mã xác thực
     * @return User object đã cập nhật
     * @throws RuntimeException nếu không tìm thấy user hoặc mã xác thực không hợp lệ
     */
    User confirmPhoneChange(Long userId, String newPhone, String verificationCode);

    /**
     * Thay đổi trạng thái của user.
     * @param userId ID của user
     * @param newStatus Trạng thái mới (ACTIVE, INACTIVE, BANNED)
     * @return User object đã cập nhật
     * @throws RuntimeException nếu không tìm thấy user
     */
    User changeUserStatus(Long userId, User.UserStatus newStatus);

    /**
     * Admin cập nhật ROLE và STATUS của driver.
     * CHỈ update 2 fields này, không động vào thông tin cá nhân
     * @param userId ID của user cần update
     * @param updateDTO DTO chứa role và status mới
     * @return User object đã cập nhật
     * @throws RuntimeException nếu không tìm thấy user hoặc role/status không hợp lệ
     */
    User adminUpdateDriverRoleAndStatus(Long userId, UpdateUserDTO updateDTO);

    /**
     * Xóa user theo ID.
     * @param id ID của user
     * @throws RuntimeException nếu không tìm thấy user
     */
    void deleteUser(Long id);

    /**
     * Ban user theo ID (cập nhật trạng thái thành BANNED).
     * @param userId ID của user cần ban
     * @param reason Lý do ban user
     * @throws RuntimeException nếu không tìm thấy user
     */
    UserDTO  reportViolation(Long userId, String reason);

    void resetPassword(String email, String newPassword);

    List<UserDTO> getAllDriver();
}
