package swp391.code.swp391.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDTO {

    @Email(message = "Invalid Email format")
    private String email;
    @Email(message = "Invalid Email format")
    private String newEmail;
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$", message = "Invalid Vietnam phone number format")
    private String newPhoneNumber;
    @Size(min = 6, max = 6, message = "Verification code must be 6 characters")
    private String verificationCode;

    private LocalDateTime expirationTime;

    public void generateVerificationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        this.verificationCode = code.toString();
        // Set expiration time to 5 minutes from now
        this.expirationTime = LocalDateTime.now().plusMinutes(5);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }
}