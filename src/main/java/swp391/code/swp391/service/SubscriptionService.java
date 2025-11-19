package swp391.code.swp391.service;

import swp391.code.swp391.dto.SubscriptionRequestDTO;
import swp391.code.swp391.dto.SubscriptionResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface định nghĩa các hành vi (service) liên quan đến quản lý Subscription.
 * Triển khai chính được thực hiện trong lớp {@link swp391.code.swp391.service.SubscriptionServiceImpl}.
 */
public interface SubscriptionService {

    /**
     * Lấy toàn bộ danh sách subscription hiện có trong hệ thống.
     *
     * @return Danh sách {@link SubscriptionResponseDTO} chứa thông tin các gói subscription.
     */
    List<SubscriptionResponseDTO> getAllSubscriptions();

    /**
     * Lấy gói subscription hiện tại của người dùng theo userId.
     *
     * @param subscriptionType ID của người dùng.
     * @return Danh sách chứa một {@link SubscriptionResponseDTO} đại diện cho gói hiện tại.
     */
   SubscriptionResponseDTO getUserSameSubscription(String subscriptionType);

    /**
     * Lấy toàn bộ các gói subscription có sẵn mà người dùng có thể đăng ký.
     *
     * @param userId ID của người dùng.
     * @return  {@link SubscriptionResponseDTO} của các gói còn khả dụng.
     */
    SubscriptionResponseDTO getCurrentSubscription(Long userId);

    /**
     * Hủy một subscription cụ thể của người dùng.
     *
     * @param subscriptionId ID của subscription cần hủy.
     * @param userId         ID của người dùng đang thực hiện hủy.
     * @return {@link SubscriptionResponseDTO} chứa thông tin gói vừa bị hủy.
     */
    SubscriptionResponseDTO cancelSubscription(Long subscriptionId, Long userId);

    /**
     * Cập nhật thông tin của một subscription plan (không liên quan đến user).
     * Dùng để admin chỉnh sửa thông tin gói cước như tên, giá, thời hạn, mô tả, v.v.
     *
     * @param subscriptionId      ID của subscription plan cần cập nhật.
     * @param subscriptionRequest DTO chứa thông tin mới để cập nhật.
     * @return {@link SubscriptionResponseDTO} chứa thông tin gói đã được cập nhật.
     */
    SubscriptionResponseDTO updateSubscriptionPlan(Long subscriptionId, SubscriptionRequestDTO subscriptionRequest);

    /**
     * Cập nhật subscription của user (mua gói mới hoặc gia hạn).
     *
     * @param subscriptionRequest DTO chứa thông tin cần thiết để cập nhật subscription của user.
     * @return {@link SubscriptionResponseDTO} đại diện cho subscription vừa cập nhật.
     */
    SubscriptionResponseDTO updateUserSubscription(SubscriptionRequestDTO subscriptionRequest);

    /**
     * Lấy giá trị của một tính năng (feature) trong gói subscription của người dùng.
     *
     * @param userId     ID của người dùng.
     * @param featureKey Khóa định danh của feature cần lấy.
     * @return Giá trị feature (dạng String).
     */
    String getFeatureValue(Long userId, String featureKey);

    /**
     * Lấy số ngày được phép đặt trước tối đa.
     * (Feature key: "ADVANCE_BOOKING_DAYS")
     *
     * @param userId ID người dùng.
     * @return Số ngày được đặt trước (int).
     */
    int getAdvanceBookingDays(Long userId);

    /**
     * Lấy phần trăm giảm giá theo gói subscription của người dùng.
     * (Feature key: "DISCOUNT_PERCENTAGE")
     *
     * @param userId ID người dùng.
     * @return Tỉ lệ giảm giá (double).
     */
    double getDiscountPercentage(Long userId);

    /**
     * Kiểm tra xem người dùng có thể tạo thêm đơn đặt phòng mới không.
     * (Feature key: "CAN_MAKE_ADDITIONAL_BOOKING")
     *
     * @param userId ID người dùng.
     * @return true nếu có thể đặt thêm, ngược lại false.
     */
    boolean canCreateMoreOrder(Long userId, int currentOrderCount);

    /**
     * Lấy số giờ tối thiểu để hủy đặt phòng.
     * (Feature key: "MIN_CANCELATION_HOURS")
     *
     * @param userId ID người dùng.
     * @return Số giờ tối thiểu (double).
     */
    double getCancelationHour(Long userId);

    /**
     * Kiểm tra xem người dùng có thể đặt lịch tại một thời điểm cụ thể không,
     * dựa trên số ngày được phép đặt trước (feature ADVANCE_BOOKING_DAYS).
     *
     * @param userId ID người dùng.
     * @param date   Ngày muốn đặt lịch.
     * @return true nếu có thể đặt, ngược lại false.
     */
    boolean canBookOnDate(Long userId, LocalDateTime date);
}
