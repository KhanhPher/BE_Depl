package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "Subscription")
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @lombok.ToString.Exclude
    @OneToMany(mappedBy = "subscription")
    private List<User> user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(name = "sub_name", nullable = false, unique = true)
    private String subscriptionName; // BASIC, PLUS, PRO

    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days")
    private Integer durationDays; // Số ngày sử dụng (30, 90, 365)

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder; // Thứ tự hiển thị

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    public enum Type {
        BASIC, PLUS, PREMIUM
    }

    @lombok.ToString.Exclude
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubscriptionFeature> features;

    @Column(name ="kwh_used",  precision = 10)
    private double kwhUsed = 0.0;
}
