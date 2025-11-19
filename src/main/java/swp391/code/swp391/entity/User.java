package swp391.code.swp391.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "verifications"})
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "full_name", nullable = false, columnDefinition = "VARCHAR(100)")
    private String fullName;
    @Column(name = "email", unique = true) //Nullable true để đăng ký bằng phone (fb)
    private String email;
    @Column(name = "password")
    private String password;
    @Column(name = "phone", unique = true) //Nullable true để đăng ký bằng email (gg)
    private String phone;
    @Column(name = "date_Of_Birth")
    private LocalDate dateOfBirth;

    @Column(name = "google_id", unique = true)
    private String googleId; // For OAuth2 Google login
    @Column(name = "facebook_id", unique = true)
    private String facebookId; // For OAuth2 Facebook login

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.DRIVER;
    @Column(name ="address", columnDefinition = "VARCHAR(255)")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "avatar")
    private String avatar;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Vehicle> vehicles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Verification> verifications;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id")
    @JsonBackReference("station-staff")
    private ChargingStation station;


    public User(String fullName, String email, String password, String phone, LocalDate dateOfBirth, String address) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }
    
    // Enums for UserStatus and UserRole
    public enum UserStatus {
        ACTIVE, INACTIVE, BANNED
    }

    public enum UserRole {
        DRIVER, ADMIN, STAFF
    }

    @Column(name = "violations", nullable = false)
    private int violations;

    @Column(name = "reason_report", columnDefinition = "TEXT")
    private String reasonReport;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "subscription_auto_renew")
    private Boolean subscriptionAutoRenew = false;

    // Helper methods trong User entity
    public boolean hasActiveSubscription() {
        if (subscription == null) return false;
        if (subscription.getSubscriptionName().equals("BASIC")) return true; // BASIC vĩnh viễn
        return subscriptionEndDate != null && subscriptionEndDate.isAfter(LocalDateTime.now());
    }

    public boolean isBasicUser() {
        return subscription == null || subscription.getSubscriptionName().equals("BASIC");
    }

    public boolean isPlusUser() {
        return subscription != null &&
                subscription.getSubscriptionName().equals("PLUS") &&
                hasActiveSubscription();
    }

    public boolean isProUser() {
        return subscription != null &&
                subscription.getSubscriptionName().equals("PRO") &&
                hasActiveSubscription();
    }

    public long getDaysRemaining() {
        if (isBasicUser()) return Long.MAX_VALUE; // Vĩnh viễn
        if (subscriptionEndDate == null) return 0;
        if (subscriptionEndDate.isBefore(LocalDateTime.now())) return 0;
        return java.time.Duration.between(LocalDateTime.now(), subscriptionEndDate).toDays();
    }
}
