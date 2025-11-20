package swp391.code.swp391.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "session")
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @OneToOne(cascade = CascadeType.ALL)
    private Order order;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double powerConsumed; //Số kwh đã sạc

    @Column(nullable = false)
    private Double baseCost; //tiền phải trả cho phiên sạc (chưa tính phí phát sinh)

    private LocalDateTime parkingStartTime; // Thời điểm bắt đầu tính phí đỗ xe

    @Column(nullable = false)
    private Boolean targetReachedNotificationSent = false; // Đã gửi thông báo đạt target battery chưa

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Fee> fees; //Các phí phát sinh

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.CHARGING;

    public enum SessionStatus {
        CHARGING,              // Đang sạc
        PARKING,               // Đã dừng sạc, đang đỗ xe (có 15 phút grace period, user tự xác nhận rời đi)
        COMPLETED              // Hoàn thành
    }

    // getTotalCost - Tính tổng chi phí bao gồm cả phí phát sinh
    public Double getTotalCost() {
        double totalFees = fees != null ?
            fees.stream()
                .mapToDouble(Fee::getAmount)
                .sum() : 0.0;
        return baseCost + totalFees;
    }
}