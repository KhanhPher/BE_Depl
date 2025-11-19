package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.CarModel;

import java.util.List;

@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    // Tìm CarModel theo brand (không phân biệt hoa thường)
    List<CarModel> findByBrandContainingIgnoreCase(String brand);

    // Tìm CarModel theo model (không phân biệt hoa thường)
    List<CarModel> findByModelContainingIgnoreCase(String model);

    // Tìm CarModel theo năm sản xuất
    List<CarModel> findByProductYear(int productYear);

    // Tìm CarModel theo ConnectorType
    @Query("SELECT cm FROM CarModel cm JOIN cm.connectorTypes ct WHERE ct.connectorTypeId = :connectorTypeId")
    List<CarModel> findByConnectorTypeId(@Param("connectorTypeId") Long connectorTypeId);

    // Kiểm tra xem CarModel có tồn tại theo brand và model
    boolean existsByBrandAndModel(String brand, String model);

}