package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import swp391.code.swp391.entity.ConnectorType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModelDTO {
    private Long carModelId;
    private String brand;
    private String model;
    private double capacity;
    private int productYear;
    private List<Long> connectorTypeIds; // Để gửi danh sách ID của ConnectorType
    private String carModelImage;
//    private List<ConnectorType> connectorTypes; // Để trả về thông tin ConnectorType
}