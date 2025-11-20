package swp391.code.swp391.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.code.swp391.entity.Fee;

import java.time.LocalDateTime;

/**
 * DTO cho chi tiết phí phạt (AC6)
 * Hiển thị: loại phí, số tiền, lý do, thời gian, trạng thái thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDetailDTO {

    private Long feeId;

    private Fee.Type feeType;

    private String feeTypeName; // Tên tiếng Việt

    private Double amount;

    private String description;

    private Boolean isPaid;

    private LocalDateTime createdAt;

    // Thông tin liên quan
    private Long orderId;

    private Long sessionId;

    private Long userId;

    private String userName;

    // Thông tin context
    private LocalDateTime orderStartTime;

    private LocalDateTime orderEndTime;

    private String chargingStationName;

    private String chargingPointName;

    /**
     * Convert Fee Type sang tên tiếng Việt
     */
    public static String getFeeTypeName(Fee.Type type) {
        return switch (type) {
            case CANCEL -> "Phí hủy lịch muộn";
            case NO_SHOW -> "Phí không đến theo lịch";
            case PARKING -> "Phí đỗ xe";
        };
    }

    /**
     * Tạo DTO từ Fee entity
     */
    public static FeeDetailDTO fromEntity(Fee fee) {
        FeeDetailDTOBuilder builder = FeeDetailDTO.builder()
                .feeId(fee.getFeeId())
                .feeType(fee.getType())
                .feeTypeName(getFeeTypeName(fee.getType()))
                .amount(fee.getAmount())
                .description(fee.getDescription())
                .isPaid(fee.getIsPaid())
                .createdAt(fee.getCreatedAt());

        // Thêm thông tin từ Order nếu có
        if (fee.getOrder() != null) {
            builder.orderId(fee.getOrder().getOrderId())
                   .orderStartTime(fee.getOrder().getStartTime())
                   .orderEndTime(fee.getOrder().getEndTime())
                   .userId(fee.getOrder().getUser().getUserId())
                   .userName(fee.getOrder().getUser().getFullName());

            if (fee.getOrder().getChargingPoint() != null) {
                builder.chargingPointName(fee.getOrder().getChargingPoint().getChargingPointName());

                if (fee.getOrder().getChargingPoint().getStation() != null) {
                    builder.chargingStationName(
                        fee.getOrder().getChargingPoint().getStation().getStationName()
                    );
                }
            }
        }

        // Thêm thông tin từ Session nếu có
        if (fee.getSession() != null) {
            builder.sessionId(fee.getSession().getSessionId());

            if (fee.getSession().getOrder() != null) {
                builder.orderId(fee.getSession().getOrder().getOrderId())
                       .orderStartTime(fee.getSession().getOrder().getStartTime())
                       .orderEndTime(fee.getSession().getOrder().getEndTime())
                       .userId(fee.getSession().getOrder().getUser().getUserId())
                       .userName(fee.getSession().getOrder().getUser().getFullName());

                if (fee.getSession().getOrder().getChargingPoint() != null) {
                    builder.chargingPointName(
                        fee.getSession().getOrder().getChargingPoint().getChargingPointName()
                    );

                    if (fee.getSession().getOrder().getChargingPoint().getStation() != null) {
                        builder.chargingStationName(
                            fee.getSession().getOrder().getChargingPoint().getStation().getStationName()
                        );
                    }
                }
            }
        }

        return builder.build();
    }
}

