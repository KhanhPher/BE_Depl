package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Fee")
@NoArgsConstructor
@AllArgsConstructor
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feeId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean isPaid = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Type {
        CHARGING,  // Phí sạc quá giờ khi pin đầy (deprecated, use OVERTIME)
        OVERTIME,  // Phí sạc quá giờ khi pin đầy (AC3: 2,000 VNĐ/phút)
        NO_SHOW,   // Phí không đến theo lịch (AC2: 30% estimated cost)
        CANCEL,    // Phí hủy muộn (AC1: 10% estimated cost)
        PARKING    // Phí đỗ xe sau khi sạc xong (tính theo thời gian thực tế)
    }
}