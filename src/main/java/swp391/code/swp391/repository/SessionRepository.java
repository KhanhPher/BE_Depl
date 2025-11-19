package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.Session;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Tìm session theo trạng thái
     */
    List<Session> findByStatus(Session.SessionStatus status);

    /**
     * Tìm session theo order ID
     */
    Session findByOrderOrderId(Long orderId);

    /**
     * Tìm session theo station ID
     */
    List<Session> findByOrderChargingPointStationStationId(Long stationId);

    // ============= ANALYTICS QUERIES =============

    /**
     * Tìm tất cả sessions trong khoảng thời gian
     */
    List<Session> findByStartTimeBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Tìm sessions theo user trong khoảng thời gian
     */
    List<Session> findByOrderUserUserIdAndStartTimeBetween(Long userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Tìm sessions theo station trong khoảng thời gian
     */
    List<Session> findByOrderChargingPointStationStationIdAndStartTimeBetween(Long stationId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Lấy top N sessions gần nhất
     */
    List<Session> findTop10ByOrderByStartTimeDesc();

    /**
     * Đếm tổng số sessions
     */
    Long countByStartTimeBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}