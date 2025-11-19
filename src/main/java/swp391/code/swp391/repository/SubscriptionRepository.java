package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.dto.SubscriptionResponseDTO;
import swp391.code.swp391.entity.Subscription;
import swp391.code.swp391.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Tìm các gói đăng ký còn hiệu lực của user
     */
    List<Subscription> findByUserAndEndDateAfter(User user, LocalDateTime currentDate);

    /**
     * Tìm tất cả gói đăng ký của user
     */
    List<Subscription> findByUserOrderByEndDateDesc(List<User> user);

    /**
     * Tìm gói đăng ký theo loại
     */
    Subscription findByType(Subscription.Type type);

    /**
     * Tìm gói đăng ký theo giá (cho subscription payment callback)
     */
    List<Subscription> findByPrice(BigDecimal price);

    /**
     * Tìm gói đăng ký theo user id (lấy subscription còn hiệu lực hoặc mới nhất)
     */
    @Query("""
    SELECT s
    FROM User u
    JOIN u.subscription s
    WHERE u.userId = :userId     \s
    ORDER BY s.endDate DESC
    LIMIT 1
""")
    Subscription findSubscriptionByUserId(@Param("userId") Long userId);


    /**
     * Lấy tất cả gói đăng ký còn hiệu lực (1 Subscription có nhiều User)
     */
    @Query("""
        SELECT DISTINCT s
        FROM Subscription s
        LEFT JOIN FETCH s.user u
        WHERE s.endDate > CURRENT_TIMESTAMP
    """)
    List<Subscription> findAllActiveSubscriptionsWithUsers();

    /**
     * Lấy gói đăng ký còn hiệu lực của 1 user cụ thể (dành cho DTO)
     */
    @Query("""
        SELECT new swp391.code.swp391.dto.SubscriptionResponseDTO(
            s.subscriptionId,
            u.userId,
            s.type,
            s.startDate,
            s.endDate
        )
        FROM User u
        JOIN u.subscription s
        WHERE u.userId = :userId
          AND s.endDate > CURRENT_TIMESTAMP
    """)
    List<SubscriptionResponseDTO> findAllAvailableSub(@Param("userId") Long userId);
}
