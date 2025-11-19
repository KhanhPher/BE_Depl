package swp391.code.swp391.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Entity
@Table(name = "connector_types")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConnectorType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connector_type_id")
    private Long connectorTypeId;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "power_output", nullable = false)
    private double powerOutput; // in kW

    @Column(name = "price_per_kwh", nullable = false)
    private double pricePerKWh; // basePrice per kWh(VND)

    @ManyToMany(mappedBy = "connectorTypes", fetch =  FetchType.LAZY)
    @JsonBackReference
    private List<CarModel> carModels;

    @OneToMany(mappedBy = "connectorType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference(value = "connectorType-chargingPoint")
    private List<ChargingPoint> chargingPoints;
}
