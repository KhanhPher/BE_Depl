package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.Fee;

import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * Tìm tất cả các khoản phí theo session ID
     */
    List<Fee> findBySessionSessionId(Long sessionId);

    /**
     * Tìm tất cả các khoản phí theo order ID
     */
    List<Fee> findByOrderOrderId(Long orderId);

    /**
     * Tìm các khoản phí chưa thanh toán
     */
    List<Fee> findByIsPaidFalse();

    /**
     * Tìm các khoản phí theo loại
     */
    List<Fee> findByType(Fee.Type type);
}