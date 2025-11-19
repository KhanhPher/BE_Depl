package swp391.code.swp391.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_factors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long priceFactorId;

    @Column(nullable = false)
    private Long stationId;

    @Column(nullable = false)
    private Double factor;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(length = 255)
    private String description;
}
