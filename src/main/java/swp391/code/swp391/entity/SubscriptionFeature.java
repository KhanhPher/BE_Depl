package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "subscription")
public class SubscriptionFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long featureId;
    @Column(name = "feature_key", nullable = false)
    private String featureKey; // advance_booking_days, discount_percentage, concurrent_bookings, etc.

    @Column(name = "feature_value", nullable = false)
    private String featureValue; // Giá trị: "3", "5", "10", "true", etc.

    @Column(name = "feature_type")
    @Enumerated(EnumType.STRING)
    private FeatureType featureType;

    @Column(name = "display_name", columnDefinition = "NVARCHAR(255)")
    private String displayName; // "Đặt lịch trước", "Giảm giá", etc.

    @Column(name = "description" , columnDefinition = "NVARCHAR(255)")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum FeatureType {
        NUMERIC,    // Giá trị số: 3, 5, 10
        BOOLEAN,    // true/false
        STRING,     // text value
        PERCENTAGE  // %
    }

    // Helper methods
    public Integer getIntValue() {
        return Integer.parseInt(featureValue);
    }

    public Double getDoubleValue() {
        return Double.parseDouble(featureValue);
    }

    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(featureValue);
    }
    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;


}
