package swp391.code.swp391.dto;

import lombok.Data;
import swp391.code.swp391.entity.Subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubscriptionRequestDTO {
    private Long userId;
    private Subscription.Type type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Additional fields for updating subscription plan information
    private String subscriptionName;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Boolean isActive;
    private Integer displayOrder;
}
