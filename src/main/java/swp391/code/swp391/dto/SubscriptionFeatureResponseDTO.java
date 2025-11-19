package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionFeatureResponseDTO {

    private Long featureId;
    private Long subscriptionId;
    private String subscriptionName;
    private String featureKey;
    private String featureValue;
    private String featureType;
    private String displayName;
    private String description;
    private LocalDateTime createdAt;
}