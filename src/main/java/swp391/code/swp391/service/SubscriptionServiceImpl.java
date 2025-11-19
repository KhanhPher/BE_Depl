package swp391.code.swp391.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.SubscriptionRequestDTO;
import swp391.code.swp391.dto.SubscriptionResponseDTO;
import swp391.code.swp391.dto.UserDTO;
import swp391.code.swp391.entity.Subscription;
import swp391.code.swp391.entity.SubscriptionFeature;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.SubscriptionFeatureRepository;
import swp391.code.swp391.repository.SubscriptionRepository;
import swp391.code.swp391.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



@Service

public class SubscriptionServiceImpl implements SubscriptionService {
    // inject repositories
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SubscriptionFeatureRepository subscriptionFeatureRepository;

    @Override
    public List<SubscriptionResponseDTO> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();

    }

    @Override
    public SubscriptionResponseDTO getUserSameSubscription(String subscriptionType){
        // Convert String to Subscription.Type enum
        Subscription.Type type;
        try {
            type = Subscription.Type.valueOf(subscriptionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid subscription type: " + subscriptionType);
        }

        Subscription subscription = subscriptionRepository.findByType(type);
        if (subscription == null) {
            throw new RuntimeException("Subscription not found with type: " + subscriptionType);
        }
        return convertToDTO(subscription);
    }

    /**
     * Trả về gói đăng kí hiện tại của người dùng
     * @param userId ID của người dùng.
     * @return
     */
    @Override
    public SubscriptionResponseDTO getCurrentSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findSubscriptionByUserId(userId);
        if (subscription == null) {
            throw new RuntimeException("No active subscription found for user with id: " + userId);
        }

        // Convert to DTO but only include the specific user, not all users in the subscription
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(subscription.getSubscriptionId());
        dto.setType(subscription.getType());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());

        // Only add the specific user who requested, not all users in this subscription
        User requestedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        dto.setUserId(Collections.singletonList(new UserDTO(requestedUser, false)));

        return dto;
    }

    /**
     * Hủy đăng kí của người dùng và chuyển về gói BASIC
     * @param subscriptionId ID của subscription cần hủy.
     * @param userId         ID của người dùng thực hiện hủy.
     * @return
     */
    @Override
    public SubscriptionResponseDTO cancelSubscription(Long subscriptionId, Long userId) {
        // Load user và kiểm tra quyền sở hữu subscription
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + subscriptionId));

        // Kiểm tra subscription có thuộc về user không
        Subscription userSubscription = user.getSubscription();
        if (userSubscription == null || userSubscription.getSubscriptionId() == null || !userSubscription.getSubscriptionId().equals(subscriptionId)) {
            throw new RuntimeException("Subscription does not belong to user with id: " + userId);
        }

        // Kiểm tra xem user đã ở gói BASIC chưa
        if (subscription.getType() == Subscription.Type.BASIC) {
            throw new RuntimeException("Cannot cancel BASIC subscription. You are already on the basic plan.");
        }

        // Tìm hoặc tạo gói BASIC
        Subscription basicSubscription = subscriptionRepository.findByType(Subscription.Type.BASIC);
        if (basicSubscription == null) {
            // Nếu chưa có gói BASIC trong DB, tạo mới
            basicSubscription = new Subscription();
            basicSubscription.setType(Subscription.Type.BASIC);
            basicSubscription.setSubscriptionName("BASIC");
            basicSubscription.setDescription("Free basic subscription");
            basicSubscription.setIsActive(true);
            basicSubscription.setStartDate(LocalDateTime.now());
            // Gói BASIC không có endDate (vô thời hạn)
            basicSubscription.setEndDate(LocalDateTime.now().plusYears(100)); // Set endDate rất xa
            basicSubscription = subscriptionRepository.save(basicSubscription);
        }

        // Chuyển user về gói BASIC
        user.setSubscription(basicSubscription);
        userRepository.save(user);

        // Trả về thông tin gói BASIC mới
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(basicSubscription.getSubscriptionId());
        dto.setType(basicSubscription.getType());
        dto.setStartDate(basicSubscription.getStartDate());
        dto.setEndDate(basicSubscription.getEndDate());
        dto.setUserId(Collections.singletonList(new UserDTO(user, false)));

        return dto;
    }

    /**
     * Cập nhật thông tin cho subscription
     * @param subscriptionId      ID của subscription plan cần cập nhật.
     * @param subscriptionRequest DTO chứa thông tin mới để cập nhật.
     * @return
     */
    @Override
    public SubscriptionResponseDTO updateSubscriptionPlan(Long subscriptionId, SubscriptionRequestDTO subscriptionRequest) {
        // Tìm subscription plan cần cập nhật
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found with id: " + subscriptionId));

        // Cập nhật các thông tin của subscription plan
        if (subscriptionRequest.getType() != null) {
            subscription.setType(subscriptionRequest.getType());
        }

        if (subscriptionRequest.getSubscriptionName() != null) {
            subscription.setSubscriptionName(subscriptionRequest.getSubscriptionName());
        }

        if (subscriptionRequest.getDescription() != null) {
            subscription.setDescription(subscriptionRequest.getDescription());
        }

        if (subscriptionRequest.getPrice() != null) {
            subscription.setPrice(subscriptionRequest.getPrice());
        }

        if (subscriptionRequest.getDurationDays() != null) {
            subscription.setDurationDays(subscriptionRequest.getDurationDays());
        }

        if (subscriptionRequest.getIsActive() != null) {
            subscription.setIsActive(subscriptionRequest.getIsActive());
        }

        if (subscriptionRequest.getDisplayOrder() != null) {
            subscription.setDisplayOrder(subscriptionRequest.getDisplayOrder());
        }

        // Cập nhật thời gian chỉnh sửa
        subscription.setUpdatedAt(LocalDateTime.now());

        // Lưu thay đổi vào database
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        // Trả về DTO
        return convertToDTO(updatedSubscription);
    }

    /**
     * Cập nhật subscription cho user
     * @param subscriptionRequest DTO chứa thông tin cần thiết để cập nhật subscription của user.
     * @return
     */
    @Override
    public SubscriptionResponseDTO updateUserSubscription(SubscriptionRequestDTO subscriptionRequest) {
        // Kiểm tra userId có được cung cấp không
        if (subscriptionRequest.getUserId() == null) {
            throw new RuntimeException("User ID is required to update subscription");
        }

        // Load user
        User user = userRepository.findById(subscriptionRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + subscriptionRequest.getUserId()));

        // Tìm subscription theo type được yêu cầu
        Subscription targetSubscription = subscriptionRepository.findByType(subscriptionRequest.getType());
        if (targetSubscription == null) {
            throw new RuntimeException("Subscription plan not found with type: " + subscriptionRequest.getType());
        }

        // Kiểm tra subscription plan có durationDays không
        if (targetSubscription.getDurationDays() == null || targetSubscription.getDurationDays() <= 0) {
            throw new RuntimeException("Invalid subscription plan: duration days not configured");
        }

        // Kiểm tra xem user đã có subscription này chưa
        Subscription currentSubscription = user.getSubscription();

        if (currentSubscription != null && currentSubscription.getType() == subscriptionRequest.getType()) {
            // Nếu user đã có gói này, cộng thêm thời gian sử dụng
            LocalDateTime currentEndDate = currentSubscription.getEndDate();
            LocalDateTime newEndDate;

            // Nếu gói hiện tại chưa hết hạn, cộng thêm từ endDate hiện tại
            if (currentEndDate != null && currentEndDate.isAfter(LocalDateTime.now())) {
                newEndDate = currentEndDate.plusDays(targetSubscription.getDurationDays());
            } else {
                // Nếu gói đã hết hạn, bắt đầu từ hiện tại
                newEndDate = LocalDateTime.now().plusDays(targetSubscription.getDurationDays());
            }

            // Cập nhật endDate cho subscription hiện tại
            currentSubscription.setEndDate(newEndDate);
            currentSubscription.setStartDate(currentSubscription.getStartDate() != null ?
                    currentSubscription.getStartDate() : LocalDateTime.now());
            currentSubscription.setUpdatedAt(LocalDateTime.now());

            Subscription updatedSubscription = subscriptionRepository.save(currentSubscription);

            // Trả về thông tin subscription đã được gia hạn
            SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
            dto.setSubscriptionId(updatedSubscription.getSubscriptionId());
            dto.setType(updatedSubscription.getType());
            dto.setStartDate(updatedSubscription.getStartDate());
            dto.setEndDate(updatedSubscription.getEndDate());
            dto.setUserId(Collections.singletonList(new UserDTO(user, false)));

            return dto;
        }

        // Nếu user chưa có gói này hoặc đang dùng gói khác, chuyển sang gói mới
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(targetSubscription.getDurationDays());

        targetSubscription.setStartDate(startDate);
        targetSubscription.setEndDate(endDate);
        targetSubscription.setUpdatedAt(LocalDateTime.now());

        // Cập nhật subscription của user (upgrade/downgrade gói)
        user.setSubscription(targetSubscription);
        userRepository.save(user);

        // Trả về thông tin subscription mới
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(targetSubscription.getSubscriptionId());
        dto.setType(targetSubscription.getType());
        dto.setStartDate(targetSubscription.getStartDate());
        dto.setEndDate(targetSubscription.getEndDate());
        dto.setUserId(Collections.singletonList(new UserDTO(user, false)));

        return dto;
    }

    public SubscriptionResponseDTO convertToDTO(Subscription subscription) {
        SubscriptionResponseDTO dto = new SubscriptionResponseDTO();
        dto.setSubscriptionId(subscription.getSubscriptionId());
        dto.setType(subscription.getType());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        // Map price
        if (subscription.getPrice() != null) {
            dto.setPrice(subscription.getPrice());
        }
        // Map subscriptionName
        if (subscription.getSubscriptionName() != null) {
            dto.setSubscriptionName(subscription.getSubscriptionName());
        }
        // Map description
        if (subscription.getDescription() != null) {
            dto.setDescription(subscription.getDescription());
        }
        // Map durationDays
        if (subscription.getDurationDays() != null) {
            dto.setDurationDays(subscription.getDurationDays());
        }
        // Map isActive
        if (subscription.getIsActive() != null) {
            dto.setIsActive(subscription.getIsActive());
        }
        // Map displayOrder
        if (subscription.getDisplayOrder() != null) {
            dto.setDisplayOrder(subscription.getDisplayOrder());
        }
        // Map users to UserDTO list (null-safe)
        if (subscription.getUser() != null) {
            dto.setUserId(subscription.getUser().stream()
                    .map(user -> new UserDTO(user, false))
                    .collect(Collectors.toList()));
        } else {
            dto.setUserId(Collections.emptyList());
        }

        return dto;


    }

    /**
     * Lấy giá trị feature của subscription cho user
     */
    @Override
    public String getFeatureValue(Long userId, String featureKey) {
        // Load user and its subscription
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Subscription subscription = user.getSubscription();
        if (subscription == null) {
            throw new RuntimeException("No current subscription found for user with id: " + userId);
        }

        // Ensure we have a managed Subscription entity with id populated
        if (subscription.getSubscriptionId() != null) {
            final Long subId = subscription.getSubscriptionId();
            subscription = subscriptionRepository.findById(subId)
                    .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + subId));
        }

        // Optionally check that subscription is still active
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Subscription has expired for user with id: " + userId);
        }

        // Save subscriptionId for lambda
        final Long subscriptionId = subscription.getSubscriptionId();

        // repository returns Optional<SubscriptionFeature>
        SubscriptionFeature feature = subscriptionFeatureRepository.findBySubscriptionAndFeatureKey(subscription, featureKey)
                .orElseThrow(() -> new RuntimeException("Feature '" + featureKey + "' not found for subscription with id: " + subscriptionId));

        return feature.getFeatureValue();
    }

    /**
     * Lấy số ngày đặt trước tối đa từ feature ADVANCE_BOOKING_DAYS
     */
    public int getAdvanceBookingDays(Long userId) {
        String value = getFeatureValue(userId, "ADVANCE_BOOKING_DAYS");
        return Integer.parseInt(value);
    }

    /**
     * Laays phần trăm giảm giá từ feature DISCOUNT_PERCENTAGE
     */
    public double getDiscountPercentage(Long userId) {
        String value = getFeatureValue(userId, "DISCOUNT_PERCENTAGE");
        return Double.parseDouble(value);
    }
    /**
     * Kiểm tra xem có thể đặt thêm không
     */
    public boolean canCreateMoreOrder(Long userId, int currentOrderCount) {
        String value = getFeatureValue(userId, "CAN_MAKE_ADDITIONAL_BOOKING");
        int maxOrders = Integer.parseInt(value);
        return currentOrderCount < maxOrders;
    }

    /**
     * Laays số giờ tối thiểu
     */
    public double getCancelationHour(Long userId) {
        String value = getFeatureValue(userId, "MIN_CANCELATION_HOURS");
        return Double.parseDouble(value);
    }

    /**
     * Kiểm tra uuser có thể đặt lịch vào ngày này không
     */
    public boolean canBookOnDate(Long userId, LocalDateTime date) {
        int maxAdvancedDays = getAdvanceBookingDays(userId);
        if (maxAdvancedDays <= 0) {
            return false; // Không thể đặt lịch trước
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestBookingDate = now.plusDays(maxAdvancedDays);
        return !date.isAfter(latestBookingDate);
    }
}
