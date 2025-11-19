package swp391.code.swp391.dto;

/**
 * Interface Projection để hứng kết quả từ query getConnectorUtilizationStats.
 * Tên các hàm (getter) phải khớp chính xác với alias (AS) trong câu SQL.
 */
public interface ConnectorUtilizationProjection {

    Long getStationId();
    String getStationName();
    Long getConnectorTypeId();
    String getTypeName();

    // Phải là Integer để khớp với kiểu CAST(COUNT(...) AS SIGNED)
    Integer getTotalPointsOfType();

    // Phải là Double để khớp với phép chia 3600.0
    Double getTotalHoursUsed();
}