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

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Fee> fees; //Các phí phát sinh

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.CHARGING;

    public enum SessionStatus {
        CHARGING,
        WAITING_STAFF_DECISION, // Chờ staff xác nhận xe đã rời hay chưa
        STILL_PARKED,           // Staff xác nhận xe vẫn đậu (đang tính phí)
        COMPLETED,
        OVERTIME
    }

    // Kiểm tra xem session có overtime không
    public boolean isOvertime() {
        if (endTime == null || order == null || order.getEndTime() == null) {
            return false;
        }
        return endTime.isAfter(order.getEndTime());
    }
}