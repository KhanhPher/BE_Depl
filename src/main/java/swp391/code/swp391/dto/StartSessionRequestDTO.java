package swp391.code.swp391.dto;

import lombok.Data;


@Data
public class StartSessionRequestDTO {
        private Long orderId;
        private Long vehicleId;

        // Thêm tọa độ người dùng để kiểm tra khoảng cách
        private Double userLatitude;   // Vĩ độ hiện tại của người dùng
        private Double userLongitude;  // Kinh độ hiện tại của người dùng
}
