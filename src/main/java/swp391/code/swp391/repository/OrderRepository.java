package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.ChargingPoint;
import swp391.code.swp391.entity.Order;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.entity.Order.Status;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByOrderId(Long orderId);
    /**
     * Tìm các orders trùng lặp thời gian với khoảng time slot cần kiểm tra
     *
     * Logic: Order trùng lặp khi:
     * - startTime của order < endTime của slot CẦN KIỂM TRA
     * - endTime của order > startTime của slot CẦN KIỂM TRA
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.chargingPoint.chargingPointId = :chargingPointId
        AND o.status IN ('BOOKED')
        AND o.startTime < :endTime
        AND o.endTime > :startTime
        """)
    List<Order> findOverlappingOrders(
            @Param("chargingPointId") Long chargingPointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    Order findByOrderIdAndUser_UserId(Long orderId, Long userId);

    // Add a method to find active orders by user
    /**
     * Tìm tất cả orders của một user
     */
    List<Order> findByUser_UserId(Long userId);

    /**
     * Tìm orders của user theo status
     */
    List<Order> findByUser_UserIdAndStatus(Long userId, Order.Status status);


    /**
     * Tìm orders của một trạm trong khoảng thời gian
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.chargingPoint.station.stationId = :stationId
        AND o.startTime >= :startDate
        AND o.endTime <= :endDate
        ORDER BY o.startTime ASC
        """)
    List<Order> findOrdersByStationAndDateRange(
            @Param("stationId") Long stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Tìm orders active của một charging point
     */
    @Query("""
        SELECT o FROM Order o 
        WHERE o.chargingPoint.chargingPointId = :chargingPointId
        AND o.status IN ('BOOKED')
        AND o.endTime > :currentTime
        ORDER BY o.startTime ASC
        """)
    List<Order> findActiveOrdersByChargingPoint(
            @Param("chargingPointId") Long chargingPointId,
            @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Đếm số lượng orders của user trong tháng
     */
//    @Query("""
//        SELECT COUNT(o) FROM Order o
//        WHERE o.user.userId = :userId
//        AND YEAR(o.createdAt) = :year
//        AND MONTH(o.createdAt) = :month
//        AND o.status != 'CANCELLED'
//        """)
//    Long countUserOrdersInMonth(
//            @Param("userId") Long userId,
//            @Param("year") int year,
//            @Param("month") int month
//    );


    @Query("""
        SELECT COUNT(o) > 0 FROM Order o 
        WHERE o.user.userId = :userId
        AND o.status IN ('CONFIRMED', 'IN_PROGRESS')
        AND o.startTime < :endTime
        AND o.endTime > :startTime
        """)
    boolean hasUserOrderInTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Kiểm tra user có order overlap tại cùng 1 station không
     * Dùng để ngăn user book nhiều orders trùng thời gian tại cùng 1 trạm
     */
    @Query("""
        SELECT COUNT(o) > 0 FROM Order o 
        WHERE o.user.userId = :userId
        AND o.chargingPoint.station.stationId = :stationId
        AND o.status IN ('BOOKED', 'CHARGING')
        AND o.startTime < :endTime
        AND o.endTime > :startTime
        """)
    boolean hasUserOrderAtSameStationInTimeRange(
            @Param("userId") Long userId,
            @Param("stationId") Long stationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Đếm số lượng orders active/upcoming của user
     * Chỉ đếm orders có status BOOKED hoặc CHARGING và endTime > currentTime (upcoming)
     */
    @Query("""
        SELECT COUNT(o) FROM Order o 
        WHERE o.user.userId = :userId
        AND o.status IN ('BOOKED', 'CHARGING')
        AND o.endTime > :currentTime
        """)
    int countActiveOrdersByUser(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);


    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Order o
        WHERE o.vehicle.id = :vehicleId
        AND o.status IN ('BOOKED', 'CHARGING')
        """)
    boolean isVehicleCurrentlyBooked(@Param("vehicleId") Long vehicleId);

    /**
     * Kiểm tra xe đang trong trạng thái CHARGING hay không (chỉ CHARGING)
     */
    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Order o
        WHERE o.vehicle.id = :vehicleId
        AND o.status = 'CHARGING'
        """)
    boolean isVehicleCurrentlyCharging(@Param("vehicleId") Long vehicleId);

    /**
     * Tìm các order bị conflict về thời gian cho một charging point cụ thể
     * Loại trừ order hiện tại (để tránh tự check với chính nó)
     */
    @Query("SELECT o FROM Order o WHERE " +
            "o.chargingPoint.chargingPointId = :chargingPointId AND " +
            "o.status = 'BOOKED' AND " +
            "o.orderId != :excludeOrderId AND " +
            "((o.startTime <= :endTime AND o.endTime >= :startTime))")
    List<Order> findConflictingOrders(
            @Param("chargingPointId") Long chargingPointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeOrderId") Long excludeOrderId
    );

    List<Order> findByChargingPoint_Station_StationId(Long stationId);

    /**
     * Kiểm tra user có order COMPLETED chưa thanh toán không
     * Order COMPLETED có session nhưng:
     * - Không có transaction (chưa thanh toán)
     * - Hoặc có transaction với status PENDING/FAILED
     */
    @Query("""
        SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM Order o
        LEFT JOIN Session s ON s.order.orderId = o.orderId
        LEFT JOIN Transaction t ON t.session.sessionId = s.sessionId
        WHERE o.user.userId = :userId
        AND o.status = 'COMPLETED'
        AND (
            t.transactionId IS NULL 
            OR t.status IN ('PENDING', 'FAILED')
        )
        """)
    boolean hasUnpaidCompletedOrders(@Param("userId") Long userId);

    Order getOrderByOrderId(Long orderId);

    @Query("SELECT o FROM Order o " +
            "WHERE o.chargingPoint.station.stationId = :stationId " +
            "AND o.status = 'BOOKED' " +
            "AND o.startTime >= :fromTime " +
            "ORDER BY o.chargingPoint.chargingPointId, o.startTime")
    List<Order> findUpcomingOrdersByStation(
            @Param("stationId") Long stationId,
            @Param("fromTime") LocalDateTime fromTime
    );

    List<Order> findByChargingPointAndStatusIn(ChargingPoint chargingPoint, List<Status> statuses);
}

