package swp391.code.swp391.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@Table(name="charging_points")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargingPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charging_point_id")
    private Long chargingPointId;

    @Column(name = "charging_point_name", nullable = false)
    private String chargingPointName; // Số thứ tự/tên của charging point tại trạm (VD: "A1", "A2", "B1")

    @ToString.Exclude
    @ManyToOne
    @JsonBackReference("station-points")
    private ChargingStation station;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "connector_type_id", nullable = false)
    private ConnectorType connectorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChargingPointStatus status = ChargingPointStatus.AVAILABLE;

    public enum ChargingPointStatus {
        AVAILABLE,
        OCCUPIED,
        OUT_OF_SERVICE,
        MAINTENANCE,
        RESERVED
    }
}
