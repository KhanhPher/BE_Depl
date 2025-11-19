package swp391.code.swp391.dto;

import lombok.Data;

@Data
public class SubscriptionPaymentRequestDTO {
    private Long userId;
    private Long subscriptionId;
    private String returnUrl;
    private String bankCode; // Optional
}
