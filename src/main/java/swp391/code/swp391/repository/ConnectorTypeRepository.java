package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.ConnectorType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorTypeRepository extends JpaRepository<ConnectorType, Long> {

    // Tìm connector type theo tên (không phân biệt hoa thường)
    List<ConnectorType> findByTypeNameContainingIgnoreCase(String typeName);

    // Tìm connector type theo tên chính xác
    Optional<ConnectorType> findByTypeName(String typeName);

    // Kiểm tra tên connector type đã tồn tại
    boolean existsByTypeName(String typeName);

//    // Tìm connector types theo charging point ID
//    List<ConnectorType> findByChargingPointChargingPointId(Long chargingPointId);

    @Query("SELECT ct FROM ConnectorType ct JOIN ct.chargingPoints cp WHERE cp.chargingPointId = :chargingPointId")
    List<ConnectorType> findByChargingPointId(@Param("chargingPointId") Long chargingPointId);

    // Custom query tìm connector types được sử dụng bởi vehicle nào đó
    @Query("SELECT DISTINCT ct FROM ConnectorType ct JOIN FETCH ct.carModels cm JOIN Vehicle v ON v.carModel = cm WHERE v.plateNumber = :plateNumber")
    List<ConnectorType> findByVehiclePlateNumber(@Param("plateNumber") String plateNumber);

//    // Tìm connector types chưa được assign cho charging point nào
//    @Query("SELECT ct FROM ConnectorType ct WHERE ct.chargingPoint IS NULL")
//    List<ConnectorType> findUnassignedConnectorTypes();

    @Query("SELECT ct FROM ConnectorType ct WHERE ct.chargingPoints IS EMPTY")
    List<ConnectorType> findUnassignedConnectorTypes();

    // Đếm số lượng vehicles sử dụng connector type
    @Query("SELECT ct, COUNT(v) FROM ConnectorType ct JOIN ct.carModels cm JOIN Vehicle v ON v.carModel = cm WHERE ct.connectorTypeId = :connectorTypeId GROUP BY ct")
    Object[] countVehiclesByConnectorType(@Param("connectorTypeId") Long connectorTypeId);

    Optional<ConnectorType> findConnectorTypeByConnectorTypeId(Long connectorTypeId);
}