package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long verificationId;
    @Column(name = "Verification_Token", nullable = false, unique = true)
    private String verificationToken;
    @Column(name = "Expiration", nullable = false)
    private LocalDateTime expiration;
    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "Verification_Type", nullable = false)
    private VerificationType verificationType;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum VerificationType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        PHONE_VERIFICATION
    }
}