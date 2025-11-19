package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final FeeRepository feeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactionHistory(TransactionFilterRequest filter) {
        log.info("Lấy lịch sử giao dịch (không phân trang) với filter: {}", filter);

        List<Transaction> transactions = findTransactionsByFilter(filter);

        // Sắp xếp nếu cần
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDirection = filter.getSortDirection() != null ? filter.getSortDirection() : "DESC";

        Comparator<Transaction> comparator = switch (sortBy) {
            case "amount" -> Comparator.comparing(Transaction::getAmount, Comparator.nullsLast(Double::compareTo));
            case "paymentTime" -> Comparator.comparing(Transaction::getPaymentTime, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        };

        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        List<TransactionHistoryDTO> historyDTOs = transactions.stream()
                .filter(Objects::nonNull)
                .sorted(comparator)
                .map(this::convertToHistoryDTO)
                .collect(Collectors.toList());

        return TransactionHistoryResponse.builder()
                .transactions(historyDTOs)
                .totalElements(historyDTOs.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailDTO getTransactionDetail(Long transactionId, Long userId) {
        log.info("Lấy chi tiết giao dịch: {}, user: {}", transactionId, userId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với ID: " + transactionId));

        // Kiểm tra quyền: chỉ admin/staff hoặc chủ giao dịch mới xem được
        if (userId != null && !transaction.getUser().getUserId().equals(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || (user.getRole() != User.UserRole.ADMIN && user.getRole() != User.UserRole.STAFF)) {
                throw new RuntimeException("Bạn không có quyền xem giao dịch này");
            }
        }

        return convertToDetailDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryResponse getUserTransactionHistory(Long userId) {
        log.info("Lấy lịch sử giao dịch của user: {}", userId);
        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .userId(userId)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();
        return getTransactionHistory(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummary(TransactionFilterRequest filter) {
        log.info("Tính summary cho giao dịch với filter: {}", filter);

        List<Transaction> transactions = findTransactionsByFilter(filter);

        long totalCount = transactions.size();

        BigDecimal totalAmount = transactions.stream()
                .map(t -> BigDecimal.valueOf(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSuccess = transactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.SUCCESS)
                .map(t -> BigDecimal.valueOf(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFailed = transactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.FAILED)
                .map(t -> BigDecimal.valueOf(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = transactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.PENDING)
                .map(t -> BigDecimal.valueOf(t.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TransactionSummaryDTO.builder()
                .totalTransactions(totalCount)
                .totalAmount(totalAmount)
                .totalSuccess(totalSuccess)
                .totalFailed(totalFailed)
                .totalPending(totalPending)
                .build();
    }

    /**
     * Tìm transactions theo các điều kiện filter (sử dụng in-memory filtering)
     */
    private List<Transaction> findTransactionsByFilter(TransactionFilterRequest filter) {
        List<Transaction> all = transactionRepository.findAll();

        return all.stream()
                .filter(t -> {
                    if (filter.getUserId() != null && (t.getUser() == null || !t.getUser().getUserId().equals(filter.getUserId()))) {
                        return false;
                    }
                    if (filter.getStatus() != null && t.getStatus() != filter.getStatus()) {
                        return false;
                    }
                    if (filter.getPaymentMethod() != null && t.getPaymentMethod() != filter.getPaymentMethod()) {
                        return false;
                    }
                    if (filter.getFromDate() != null && (t.getCreatedAt() == null || t.getCreatedAt().isBefore(filter.getFromDate()))) {
                        return false;
                    }
                    if (filter.getToDate() != null && (t.getCreatedAt() == null || t.getCreatedAt().isAfter(filter.getToDate()))) {
                        return false;
                    }
                    if (filter.getStationId() != null) {
                        Session s = t.getSession();
                        if (s == null || s.getOrder() == null || s.getOrder().getChargingPoint() == null
                                || s.getOrder().getChargingPoint().getStation() == null
                                || !filter.getStationId().equals(s.getOrder().getChargingPoint().getStation().getStationId())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert Transaction entity sang TransactionHistoryDTO
     */
    private TransactionHistoryDTO convertToHistoryDTO(Transaction transaction) {
        Session session = transaction.getSession();
        User user = transaction.getUser();

        String stationName = "N/A";
        String stationAddress = "N/A";

        if (session != null && session.getOrder() != null &&
                session.getOrder().getChargingPoint() != null &&
                session.getOrder().getChargingPoint().getStation() != null) {
            ChargingStation station = session.getOrder().getChargingPoint().getStation();
            stationName = station.getStationName();
            stationAddress = station.getAddress();
        }

        return TransactionHistoryDTO.builder()
                .transactionId(transaction.getTransactionId())
                .amount(BigDecimal.valueOf(transaction.getAmount()))
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .paymentTime(transaction.getPaymentTime())
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .sessionId(session != null ? session.getSessionId() : null)
                .sessionStartTime(session != null ? session.getStartTime() : null)
                .sessionEndTime(session != null ? session.getEndTime() : null)
                .powerConsumed(session != null ? BigDecimal.valueOf(session.getPowerConsumed()) : BigDecimal.ZERO)
                .stationName(stationName)
                .stationAddress(stationAddress)
                .vnpayTransactionNo(transaction.getVnpayTransactionNo())
                .vnpayBankCode(transaction.getVnpayBankCode())
                .build();
    }

    /**
     * Convert Transaction entity sang TransactionDetailDTO
     */
    private TransactionDetailDTO convertToDetailDTO(Transaction transaction) {
        Session session = transaction.getSession();
        User user = transaction.getUser();

        // Lấy payment detail
        PaymentDetailDTO paymentDetail = paymentService.getPaymentDetail(
                session.getSessionId(),
                user.getUserId()
        );

        // Lấy thông tin xe
        String vehiclePlateNumber = null;
        String vehicleBrand = null;
        String vehicleModel = null;

        if (session.getOrder() != null && session.getOrder().getVehicle() != null) {
            Vehicle vehicle = session.getOrder().getVehicle();
            vehiclePlateNumber = vehicle.getPlateNumber();
            if (vehicle.getCarModel() != null) {
                vehicleBrand = vehicle.getCarModel().getBrand();
                vehicleModel = vehicle.getCarModel().getModel();
            }
        }

        // Lấy thông tin connector type
        String connectorTypeName = null;
        if (session.getOrder() != null &&
                session.getOrder().getChargingPoint() != null &&
                session.getOrder().getChargingPoint().getConnectorType() != null) {
            connectorTypeName = session.getOrder().getChargingPoint().getConnectorType().getTypeName();
        }

        return TransactionDetailDTO.builder()
                .transactionId(transaction.getTransactionId())
                .amount(BigDecimal.valueOf(transaction.getAmount()))
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .paymentTime(transaction.getPaymentTime())
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .sessionId(session != null ? session.getSessionId() : null)
                .sessionStartTime(session != null ? session.getStartTime() : null)
                .sessionEndTime(session != null ? session.getEndTime() : null)
                .powerConsumed(paymentDetail.getPowerConsumed())
                .baseCost(paymentDetail.getBaseCost())
                .stationId(session.getOrder().getChargingPoint().getStation().getStationId())
                .stationName(paymentDetail.getStationName())
                .stationAddress(paymentDetail.getStationAddress())
                .connectorTypeName(connectorTypeName)
                .basePrice(paymentDetail.getBasePrice())
                .subscriptionDiscount(paymentDetail.getSubscriptionDiscount())
                .fees(paymentDetail.getFees())
                .totalFees(paymentDetail.getTotalFees())
                .vnpayTransactionNo(transaction.getVnpayTransactionNo())
                .vnpayBankCode(transaction.getVnpayBankCode())
                .vnpayCardType(transaction.getVnpayCardType())
                .vehiclePlateNumber(vehiclePlateNumber)
                .vehicleBrand(vehicleBrand)
                .vehicleModel(vehicleModel)
                .build();
    }
}