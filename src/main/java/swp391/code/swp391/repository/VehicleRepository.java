package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.Vehicle;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // Tìm vehicle theo user ID
    List<Vehicle> findByUserUserId(Long userId);

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    // Custom query tìm vehicle theo connector type
    @Query("SELECT v FROM Vehicle v JOIN v.carModel cm JOIN cm.connectorTypes ct WHERE ct.connectorTypeId = :connectorTypeId")
    List<Vehicle> findByCarModelConnectorTypeId(@Param("connectorTypeId") Long connectorTypeId);

    // Kiểm tra vehicle có tồn tại với plate number
    boolean existsByPlateNumber(String plateNumber);

    void deleteByPlateNumber(String plateNumber);
}