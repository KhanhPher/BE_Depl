package swp391.code.swp391.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.UpdateUserDTO;
import org.springframework.web.server.ResponseStatusException;
import swp391.code.swp391.dto.LoginRequestDTO;
import swp391.code.swp391.dto.RegisterRequestDTO;
import swp391.code.swp391.dto.UserDTO;
import swp391.code.swp391.entity.CustomUserDetails;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.SubscriptionRepository;
import swp391.code.swp391.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static swp391.code.swp391.entity.Subscription.Type.BASIC;


@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    // Regular expressions for validation
    /** Pattern for validating email addresses */
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    /** Pattern for validating Vietnamese phone numbers */
    private static final String VIETNAM_PHONE_REGEX = "^(0|\\+84)([35789])[0-9]{8}$";
    private static int MAX_VIOLATIONS = 3;

    // Dependencies
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    // Lưu trữ mã xác thực tạm thời (trong thực tế nên dùng Redis)
    private final Map<String, VerificationData> verificationCodes = new ConcurrentHashMap<>();
    // =============== AUTHENTICATION METHODS ===============

    /**
     * Loads user details by username (email or phone) for authentication
     * @param username Email or phone number of the user
     * @return UserDetails object if user found
     * @throws UsernameNotFoundException if user not found
     */

    // =============== USER MANAGEMENT METHODS ===============
    /**
     * Registers a new user in the system
     * @param registerDTO Contains user registration details
     * @return User ID if registration successful, -1 if failed
     * @throws IllegalArgumentException if email already exists
     */
    public Long registerUser(RegisterRequestDTO registerDTO) {
        String username = registerDTO.getEmail();
        // Check for existing email
        if (checkUserExistsByEmail(username)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmedPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Hash password and create new user
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
        User user = new User(registerDTO.getFullName(), username, encodedPassword, null, null, null);
        user.setSubscription(subscriptionRepository.findByType(BASIC));
        return addUser(user);
    }

    /**
     * Validates email format using regex pattern
     */
    @Override
    public boolean isValidEmail(String email) {
        return email != null && Pattern.matches(EMAIL_REGEX, email);
    }

    /**
     * Validates Vietnamese phone number format using regex pattern
     */
    @Override
    public boolean isValidVietnamPhone(String phone) {
        return phone != null && Pattern.matches(VIETNAM_PHONE_REGEX, phone);
    }

    /**
     * Saves a new user and returns their ID
     */
    @Override
    public Long addUser(User user) {
        return userRepository.save(user).getUserId();
    }

    public User getUserByPhone(String phone){
        return userRepository.getUserByPhone(phone);
    }

    public User getUserByMail(String email){
        return userRepository.getUserByEmail(email);
    }
    // User existence check methods
    @Override
    public boolean checkUserExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean checkUserExistsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    /**
     * 1. XEM THÔNG TIN USER
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));
    }

    /**
     * Lấy thông tin user dạng DTO - BAO GỒM vehicles
     */
    @Override
    public UserDTO getUserByIdWithVehicles(Long id) {
        // Sử dụng EntityGraph để fetch vehicles cùng lúc (tránh N+1 query)
        User user = userRepository.findWithVehiclesByUserId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + id));

        // Chuyển đổi User entity sang UserDTO (có vehicles)
        return new UserDTO(user, true);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }



    /**
     * 2. CẬP NHẬT THÔNG TIN CƠ BẢN (không cần xác thực)
     */
    @Transactional
    public User updateUserProfile(Long userId, UpdateUserDTO updateDTO) {
        User user = getUserById(userId);

        // Cập nhật các thông tin an toàn
        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }

        if (updateDTO.getAddress() != null) {
            user.setAddress(updateDTO.getAddress());
        }

        if (updateDTO.getPhoneNumber() != null) {
            user.setPhone(updateDTO.getPhoneNumber());
        }

        if (updateDTO.getDateOfBirth() != null) {
            user.setDateOfBirth(updateDTO.getDateOfBirth());
        }

        return userRepository.save(user);
    }

    /**
     * 2.1 CẬP NHẬT PASSWORD (không cần xác thực)
     */
    public User changePassword(Long userId, UpdateUserDTO dto) {
        User user = getUserById(userId);

        if (dto.getOldPassword() == null || dto.getNewPassword() == null || dto.getConfirmNewPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin mật khẩu");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và nhập lại không khớp");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        return userRepository.save(user);
    }


    /**
     * 2.2 CẬP NHẬT EMAIL (sau khi xác thực OTP)
     */
    public User changeEmail(Long userId, String newEmail) {
        //  Lấy thông tin user từ DB
        User user = getUserById(userId);

        //  Kiểm tra email mới có hợp lệ không
        if (newEmail == null || newEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mới không được để trống");
        }

        //  Kiểm tra nếu email mới trùng với email cũ
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mới trùng với email hiện tại");
        }

        //  Kiểm tra xem email mới đã tồn tại trong hệ thống chưa
        if (userRepository.existsByEmail(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã được sử dụng");
        }

        //  Cập nhật email mới
        user.setEmail(newEmail);

        //  Lưu vào DB
        return userRepository.save(user);
    }




    /**
     * 4A. GỬI MÃ XÁC THỰC SỐ ĐIỆN THOẠI
     */
    public String sendPhoneVerification(Long userId, String newPhone) {
        // Kiểm tra user tồn tại
        User user = getUserById(userId);

        // Kiểm tra số điện thoại hợp lệ
        if (!isValidVietnamPhone(newPhone)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        if (userRepository.existsByPhone(newPhone)) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi user khác");
        }

        // Kiểm tra không trùng với phone hiện tại
        if (newPhone.equals(user.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại mới không được trùng với số hiện tại");
        }

        // Tạo mã xác thực
        String verificationCode = generateVerificationCode();

        // Lưu mã xác thực
        String key = userId + "_" + newPhone + "_PHONE";
        verificationCodes.put(key, new VerificationData(
                verificationCode,
                newPhone,
                LocalDateTime.now().plusMinutes(15)
        ));

        // Gửi SMS (giả lập)
        sendSMSCode(newPhone, verificationCode, user.getFullName());

        return "Mã xác thực đã được gửi đến: " + maskPhoneNumber(newPhone);
    }

    /**
     * 4B. XÁC THỰC VÀ CẬP NHẬT SỐ ĐIỆN THOẠI
     */
    public User confirmPhoneChange(Long userId, String newPhone, String verificationCode) {
        User user = getUserById(userId);

        // Kiểm tra mã xác thực
        String key = userId + "_" + newPhone + "_PHONE";
        VerificationData verificationData = verificationCodes.get(key);

        if (verificationData == null) {
            throw new IllegalArgumentException("Không tìm thấy mã xác thực. Vui lòng gửi lại mã.");
        }

        if (verificationData.isExpired()) {
            verificationCodes.remove(key);
            throw new IllegalArgumentException("Mã xác thực đã hết hạn. Vui lòng gửi lại mã.");
        }

        if (!verificationData.getCode().equals(verificationCode)) {
            throw new IllegalArgumentException("Mã xác thực không đúng");
        }

        // Kiểm tra phone lần nữa
        if (userRepository.existsByPhone(newPhone)) {
            verificationCodes.remove(key);
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi user khác");
        }

        // Cập nhật số điện thoại
        user.setPhone(newPhone);
        User savedUser = userRepository.save(user);

        // Xóa mã xác thực đã sử dụng
        verificationCodes.remove(key);

        return savedUser;
    }

    /**
     * 5. THAY ĐỔI TRẠNG THÁI USER (admin only)
     */
    public User changeUserStatus(Long userId, User.UserStatus newStatus) {
        User user = getUserById(userId);
        user.setStatus(newStatus);
        return userRepository.save(user);
    }

    /**
     * 5.1 ADMIN CẬP NHẬT ROLE VÀ STATUS CỦA DRIVER
     * CHỈ update role và status, KHÔNG động vào thông tin cá nhân
     */
    public User adminUpdateDriverRoleAndStatus(Long userId, UpdateUserDTO updateDTO) {
        User user = getUserById(userId);

        // CHỈ update role và status
        if (updateDTO.getRole() != null) {
            try {
                user.setRole(User.UserRole.valueOf(updateDTO.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role không hợp lệ: " + updateDTO.getRole());
            }
        }

        if (updateDTO.getStatus() != null) {
            try {
                User.UserStatus newStatus = User.UserStatus.valueOf(updateDTO.getStatus().toUpperCase());
                user.setStatus(newStatus);

                // Khi unban user (set status = ACTIVE), reset violations về 0 và clear reason
                if (newStatus == User.UserStatus.ACTIVE && user.getViolations() > 0) {
                    user.setViolations(0);
                    user.setReasonReport(null); // Clear lý do ban
                }

                // Nếu ban user, lưu lý do
                if (newStatus == User.UserStatus.BANNED && updateDTO.getReasonReport() != null) {
                    user.setReasonReport(updateDTO.getReasonReport());
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status không hợp lệ: " + updateDTO.getStatus());
            }
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
            User user = getUserById(userId);
            user.setStatus(User.UserStatus.INACTIVE);
            userRepository.save(user);
    }

    @Override
    public UserDTO reportViolation(Long userId, String reason) {
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));
        int newViolationCount = user.getViolations() + 1;
        user.setViolations(newViolationCount);
        if (newViolationCount >= MAX_VIOLATIONS) {
            user.setStatus(User.UserStatus.BANNED);
            user.setReasonReport(reason);
        }
        return convertToDTO(userRepository.save(user));
    }

    public UserDTO convertToDTO(User user) {
        return new UserDTO(user);
    }
    // =============== HELPER METHODS ===============

    /**
     * Tạo mã xác thực 6 số ngẫu nhiên
     */
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }



    /**
     * Ẩn một phần email để bảo mật
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) return email;

        String maskedUsername = username.charAt(0) +
                "*".repeat(username.length() - 2) +
                username.charAt(username.length() - 1);

        return maskedUsername + "@" + domain;
    }

    /**
     * Ẩn một phần số điện thoại để bảo mật
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return phone;

        return phone.substring(0, 3) +
                "*".repeat(phone.length() - 6) +
                phone.substring(phone.length() - 3);
    }



    /**
     * Giả lập gửi SMS
     */
    private void sendSMSCode(String phone, String code, String fullName) {
        System.out.println("=== SENDING SMS ===");
        System.out.println("To: " + phone);
        System.out.println("Message: Xin chào " + fullName + ". Mã xác thực của bạn là: " + code + ". Có hiệu lực 15 phút.");
        System.out.println("===================");

        // TODO: Implement real SMS sending
        // smsService.sendVerificationCode(phone, code, fullName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllDriver(){
        return userRepository.findUsersByRole(User.UserRole.DRIVER).stream()
                .map(this:: convertToDTO)
                .collect(Collectors.toList());
    }

    // =============== INNER CLASSES ===============

    /**
     * Class lưu trữ thông tin mã xác thực
     */
    private static class VerificationData {
        @Getter
        private final String code;
        @Getter
        private final String newValue;
        private final LocalDateTime expiryTime;

        public VerificationData(String code, String newValue, LocalDateTime expiryTime) {
            this.code = code;
            this.newValue = newValue;
            this.expiryTime = expiryTime;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
    /**
     * 6. Forgot password
     */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

}