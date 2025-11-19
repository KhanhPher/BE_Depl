package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.ChargingStation;
import swp391.code.swp391.entity.ChargingStation.ChargingStationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    // Tìm charging stations theo tên (không phân biệt hoa thường)
    List<ChargingStation> findByStationNameContainingIgnoreCase(String stationName);

    // Tìm charging stations theo địa chỉ (không phân biệt hoa thường)
    List<ChargingStation> findByAddressContainingIgnoreCase(String address);

    // Tìm charging stations theo status
    List<ChargingStation> findByStatus(ChargingStationStatus status);

    // Tìm charging station theo tên chính xác
    Optional<ChargingStation> findByStationName(String stationName);

    // Tìm charging station theo id chính xác
    Optional<ChargingStation> findByStationId(Long stationId);

    // Kiểm tra station ID đã tồn tại
    boolean existsByStationId(Long stationId);

    // Kiểm tra tên station đã tồn tại
    boolean existsByStationName(String stationName);

    // Đếm số lượng charging stations theo status
    long countByStatus(ChargingStationStatus status);

    // Custom query: Tìm stations có charging points available
    @Query("SELECT DISTINCT cs FROM ChargingStation cs JOIN cs.chargingPoint cp WHERE cp.status = 'AVAILABLE'")
    List<ChargingStation> findStationsWithAvailableChargingPoints();

    // Custom query: Tìm stations theo số lượng charging points
    @Query("SELECT cs FROM ChargingStation cs WHERE SIZE(cs.chargingPoint) >= :minPoints")
    List<ChargingStation> findStationsWithMinimumChargingPoints(@Param("minPoints") int minPoints);

    // Custom query: Tìm stations không có charging points
    @Query("SELECT cs FROM ChargingStation cs WHERE SIZE(cs.chargingPoint) = 0")
    List<ChargingStation> findStationsWithoutChargingPoints();

    // Custom query: Lấy stations với số lượng charging points
    @Query("SELECT cs, SIZE(cs.chargingPoint) FROM ChargingStation cs")
    List<Object[]> findStationsWithChargingPointCount();

    // Custom query: Tìm stations theo connector type
    @Query("SELECT DISTINCT cs FROM ChargingStation cs JOIN cs.chargingPoint cp JOIN cp.connectorType ct WHERE ct.connectorTypeId = :connectorTypeId")
    List<ChargingStation> findStationsByConnectorType(@Param("connectorTypeId") Long connectorTypeId);

    // Custom query: Tìm stations theo địa chỉ và status
    List<ChargingStation> findByAddressContainingIgnoreCaseAndStatus(String address, ChargingStationStatus status);

    // Custom query: Tìm stations theo tên và status
    List<ChargingStation> findByStationNameContainingIgnoreCaseAndStatus(String stationName, ChargingStationStatus status);

    // Tìm station mà staff đang quản lý
    @Query("SELECT s FROM ChargingStation s WHERE :staffId MEMBER OF s.staff_id")
    List<ChargingStation> findByStaffIdContains(@Param("staffId") Long staffId);

    // Tìm stationId theo tên station
    @Query("SELECT cs.stationId FROM ChargingStation cs WHERE cs.stationName = :stationName")
    Long findStationIdByName(@Param("stationName") String stationName);
}