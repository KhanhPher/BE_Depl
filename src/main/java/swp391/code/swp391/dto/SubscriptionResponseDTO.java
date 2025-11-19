package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

import swp391.code.swp391.entity.Subscription;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDTO {
    private Long subscriptionId;
    private List<UserDTO> userId;
    private Subscription.Type type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal price;
    private String subscriptionName;
    private String description;
    private Integer durationDays;
    private Boolean isActive;
    private Integer displayOrder;

    // Constructor for JPQL query with single userId
    public SubscriptionResponseDTO(Long subscriptionId, Long singleUserId, Subscription.Type type, LocalDateTime startDate, LocalDateTime endDate) {
        this.subscriptionId = subscriptionId;
        this.userId = Collections.emptyList(); // Will be populated later if needed
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
