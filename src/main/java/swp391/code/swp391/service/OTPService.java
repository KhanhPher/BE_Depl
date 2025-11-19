package swp391.code.swp391.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.SendOTPRequest;
import swp391.code.swp391.service.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final EmailService emailService;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom random = new SecureRandom();
    SendOTPRequest.OtpPurpose purpose;

    private final Map<String, OTPData> registrationOtpStore = new ConcurrentHashMap<>();
    private final Map<String, OTPData> emailChangeOtpStore = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class OTPData {
        private String otpCode;
        private LocalDateTime expiresAt;
        private Long userId;

        public OTPData(String otpCode, LocalDateTime expiresAt) {
            this.otpCode = otpCode;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public void generateAndSendOTPForRegistration(String email) {
        String otpCode = generateOTPCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        purpose = SendOTPRequest.OtpPurpose.FORGOT_PASSWORD;

        registrationOtpStore.put(email, new OTPData(otpCode, expiresAt));
        emailService.sendOTPEmail(email, otpCode, OTP_EXPIRY_MINUTES, purpose);

        //log.info("OTP đăng ký đã gửi đến: {}", email);
    }

    public void generateAndSendOTPForEmailChange(String newEmail, Long userId) {
        String otpCode = generateOTPCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        purpose = SendOTPRequest.OtpPurpose.CHANGE_EMAIL;

        String key = userId + ":" + newEmail;
        emailChangeOtpStore.put(key, new OTPData(otpCode, expiresAt, userId));
        emailService.sendOTPEmail(newEmail, otpCode, OTP_EXPIRY_MINUTES, purpose);

        log.info("OTP đổi email đã gửi đến: {} cho user: {}", newEmail, userId);
    }

    public boolean verifyOTPForRegistration(String email, String otpCode) {
        OTPData otpData = registrationOtpStore.get(email);

        if (otpData == null) {
            log.warn("Không tìm thấy OTP cho: {}", email);
            return false;
        }

        if (otpData.isExpired()) {
            log.warn("OTP đã hết hạn cho: {}", email);
            registrationOtpStore.remove(email);
            return false;
        }

        if (!otpData.getOtpCode().equals(otpCode)) {
            log.warn("Mã OTP không đúng cho: {}", email);
            return false;
        }

        registrationOtpStore.remove(email);
        log.info("OTP xác thực thành công cho: {}", email);
        return true;
    }

    public boolean verifyOTPForEmailChange(String newEmail, Long userId, String otpCode) {
        String key = userId + ":" + newEmail;
        OTPData otpData = emailChangeOtpStore.get(key);

        if (otpData == null || otpData.isExpired()) {
            emailChangeOtpStore.remove(key);
            return false;
        }

        if (!otpData.getOtpCode().equals(otpCode)) {
            return false;
        }

        emailChangeOtpStore.remove(key);
        log.info("OTP đổi email xác thực thành công: {} user: {}", newEmail, userId);
        return true;
    }

    private String generateOTPCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredOTPs() {
        registrationOtpStore.entrySet().removeIf(e -> e.getValue().isExpired());
        emailChangeOtpStore.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}