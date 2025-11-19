package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "car_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long car_model_id;
    @Column(name = "brand")
    private String brand;
    @Column(name = "model")
    private String model;
    @Column(name = "capacity")
    private double capacity; //kWh
    @Column(name = "product_year")
    private int productYear;
    @Column(name= "img_url")
    private String img_url;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "carmodel_connectortype",
            joinColumns = @JoinColumn(name = "car_model_id"),
            inverseJoinColumns = @JoinColumn(name = "connector_type_id")
    )
    @ToString.Exclude
    private List<ConnectorType> connectorTypes;
}
