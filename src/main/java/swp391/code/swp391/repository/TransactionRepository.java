package swp391.code.swp391.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.Session;
import swp391.code.swp391.entity.Transaction;
import swp391.code.swp391.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Tìm transaction theo session và user
     */
    Optional<Transaction> findBySessionAndUser(Session session, User user);

    /**
     * Tìm tất cả transaction của user
     */
    List<Transaction> findByUserOrderByTransactionIdDesc(User user);

    /**
     * Tìm transaction theo trạng thái
     */
    List<Transaction> findByStatus(Transaction.Status status);

    /**
     * Tìm transaction theo phương thức thanh toán
     */
    List<Transaction> findByPaymentMethod(Transaction.PaymentMethod paymentMethod);

    //Transaction History
    /**
     * Tìm transactions của user với pagination
     */
    Page<Transaction> findByUser(User user, Pageable pageable);

    /**
     * Tìm transactions của user theo status
     */
    Page<Transaction> findByUserAndStatus(User user, Transaction.Status status, Pageable pageable);

    /**
     * Tìm transactions của user theo payment method
     */
    Page<Transaction> findByUserAndPaymentMethod(User user, Transaction.PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Tìm transactions của user theo status và payment method
     */
    Page<Transaction> findByUserAndStatusAndPaymentMethod(
            User user,
            Transaction.Status status,
            Transaction.PaymentMethod paymentMethod,
            Pageable pageable
    );

    /**
     * Tìm transactions trong khoảng thời gian
     */
    Page<Transaction> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    /**
     * Tìm transactions của user trong khoảng thời gian
     */
    Page<Transaction> findByUserAndCreatedAtBetween(
            User user,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    /**
     * Tìm transactions theo status với pagination
     */
    Page<Transaction> findByStatus(Transaction.Status status, Pageable pageable);

    /**
     * Tìm transactions theo payment method với pagination
     */
    Page<Transaction> findByPaymentMethod(Transaction.PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Tìm transactions theo station ID
     */
    List<Transaction> findBySessionOrderChargingPointStationStationIdOrderByCreatedAtDesc(Long stationId);
}