package swp391.code.swp391.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.SubscriptionFeature;

import java.time.LocalDateTime;

// Request DTO để tạo/update feature
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionFeatureDTO {

    private Long featureId; // Null khi create, có giá trị khi update

    @NotNull(message = "Subscription ID không được để trống")
    private Long subscriptionId;

    @NotBlank(message = "Feature key không được để trống")
    private String featureKey;

    @NotBlank(message = "Feature value không được để trống")
    private String featureValue;

    @NotNull(message = "Feature type không được để trống")
    private SubscriptionFeature.FeatureType featureType;

    private String displayName;

    private String description;
}



