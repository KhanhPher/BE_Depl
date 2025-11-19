package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.dto.ConnectorUtilizationProjection;
import swp391.code.swp391.entity.User; // Hoặc entity chính của bạn

import java.util.List;

/**
 * Repository này chứa các câu query phức tạp cho cả 2 tính năng AI
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<User, Long> { // Entity gốc có thể khác

    /**
     * TÍNH NĂNG 2 (AI Rule-Based): Lấy hiệu suất sử dụng của từng loại trụ
     * tại từng trạm trong 30 ngày qua.
     * (Cú pháp TIME_TO_SEC/3600.0 là của MySQL)
     */
    @Query(value = """
        WITH PointStats AS (
            -- Đếm số lượng trụ
            SELECT
                cp.station_station_id AS station_id,
                cp.connector_type_id,
                ct.type_name,
                CAST(COUNT(cp.charging_point_id) AS SIGNED) AS total_points
            FROM charging_points cp
            JOIN connector_types ct ON cp.connector_type_id = ct.connector_type_id
            GROUP BY cp.station_station_id, cp.connector_type_id, ct.type_name
        ),
        UsageStats AS (
            -- Tính tổng số giờ sử dụng
            SELECT
                cp.station_station_id AS station_id,
                cp.connector_type_id,
                SUM(TIME_TO_SEC(s.end_time) - TIME_TO_SEC(s.start_time)) / 3600.0 AS total_hours_used
            FROM session s
            JOIN orders o ON s.order_order_id = o.order_id
            JOIN charging_points cp ON o.charging_point_id = cp.charging_point_id
            WHERE 
                s.status = 'COMPLETED' 
                AND s.start_time >= (NOW() - INTERVAL 30 DAY)
            GROUP BY cp.station_station_id, cp.connector_type_id
        )
        -- Kết hợp 2 bảng
        SELECT
            ps.station_id AS stationId,
            cs.station_name AS stationName,
            ps.connector_type_id AS connectorTypeId,
            ps.type_name AS typeName,
            ps.total_points AS totalPointsOfType,
            COALESCE(us.total_hours_used, 0.0) AS totalHoursUsed
        FROM PointStats ps
        JOIN charging_stations cs ON ps.station_id = cs.station_id
        LEFT JOIN UsageStats us ON ps.station_id = us.station_id AND ps.connector_type_id = us.connector_type_id
        ORDER BY cs.station_name, ps.type_name;
        """, nativeQuery = true)
    List<ConnectorUtilizationProjection> getConnectorUtilizationStats();

}