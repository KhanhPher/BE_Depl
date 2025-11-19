package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============= 1. XEM THÔNG TIN =============

    // Tìm user theo email
    Optional<User> findByEmail(String email);

    // Tìm user theo phone
    Optional<User> findByPhone(String phone);

    // ============= 2. CẬP NHẬT HỢP LỆ =============
    // Sử dụng save() có sẵn từ JpaRepository

    // ============= 3. THAY ĐỔI EMAIL/SỐ ĐIỆN THOẠI - XÁC THỰC =============

    // Kiểm tra email có tồn tại không
    boolean existsByEmail(String email);

    // Kiểm tra phone có tồn tại không
    boolean existsByPhone(String phone);

    // Kiểm tra email có tồn tại ở user khác không (loại trừ user hiện tại)
    boolean existsByEmailAndUserIdNot(String email, Long id);

    // Kiểm tra phone có tồn tại ở user khác không (loại trừ user hiện tại)
    boolean existsByPhoneAndUserIdNot(String phone, Long id);

    // Cập nhật mật khẩu mới

    //join bảng để lấy thông tin vehicles của user
    @EntityGraph(attributePaths = {"vehicles"})
    Optional<User> findWithVehiclesByUserId(Long id);

    User getUserByPhone(String phone);

    User getUserByEmail(String email);

    List<User> findByRole(User.UserRole role);
    
    // Tìm user theo status
    List<User> findByStatus(User.UserStatus status);

    // ============= STAFF MANAGEMENT =============

    // Tìm tất cả staff tại một trạm cụ thể
    List<User> findByStation_StationId(Long stationId);

    // Tìm staff theo role và status
    List<User> findByRoleAndStatus(User.UserRole role, User.UserStatus status);
    List<User> findUsersByRole(User.UserRole role);
}