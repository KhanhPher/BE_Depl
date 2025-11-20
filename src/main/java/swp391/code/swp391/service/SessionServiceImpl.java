package swp391.code.swp391.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.SessionProgressDTO;
import swp391.code.swp391.dto.SessionDTO;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final OrderRepository orderRepository;
    private final ChargingPointRepository chargingPointRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FeeRepository feeRepository;
    private final FeeCalculationService feeCalculationService;
    // private final SessionWebSocketService sessionWebSocketService;

    // Khoảng cách tối đa cho phép (tính bằng mét)
    private static final double MAX_DISTANCE_METERS = 1000.0; // 1km

    @Override
    public boolean isValidTime(Long orderId, int maxStartDelayMinutes) {
        maxStartDelayMinutes = 15; // Giới hạn thời gian bắt đầu sạc sau khi tạo order
        var order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            return false; // Order not found, so time is not valid
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(order.getStartTime()) && now.isBefore(order.getStartTime().plusMinutes(maxStartDelayMinutes));
    }

    /**
     * Tính khoảng cách giữa 2 tọa độ sử dụng công thức Haversine
     * @return khoảng cách tính bằng mét
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // Bán kính trái đất tính bằng mét

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Khoảng cách tính bằng mét
    }

    // US10: Bắt đầu phiên sạc
    @Transactional
    @Override
    public Long startSession(Long userId, Long orderId, Long vehicleId, Double userLatitude, Double userLongitude) {

        // Validate location parameters
        if (userLatitude == null || userLongitude == null) {
            throw new RuntimeException("User location is required to start charging session");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE ||
                (user.getRole() != User.UserRole.DRIVER && user.getRole() != User.UserRole.STAFF)) {
            throw new RuntimeException("Invalid user account or role");
        }

        // Kiểm tra order - THÊM KIỂM TRA NULL
        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        } else if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized for this order");
        } else if (order.getStatus() != Order.Status.BOOKED) {
            throw new RuntimeException("Order not in BOOKED status");
        }

        if (!isValidTime(orderId,15)) { // Ví dụ khung giờ ±10p
            // Áp dụng phạt no-show
            applyPenalty(order, Fee.Type.NO_SHOW);
            order.setStatus(Order.Status.CANCELED);
            orderRepository.save(order);
            throw new RuntimeException("Out of booking time slot - Order canceled with penalty");
        }

        // Kiểm tra charging point
        ChargingPoint point = chargingPointRepository.findById(order.getChargingPoint().getChargingPointId())
                .orElseThrow(() -> new RuntimeException("Charging point not found"));
        if (point.getStatus() != ChargingPoint.ChargingPointStatus.AVAILABLE) {
            throw new RuntimeException("Charging point not available");
        }

        // ===== KIỂM TRA KHOẢNG CÁCH - TÍNH NĂNG MỚI =====
        ChargingStation station = point.getStation();
        if (station.getLatitude() == 0.0 && station.getLongitude() == 0.0) {
            throw new RuntimeException("Station location not configured");
        }

        double distance = calculateDistance(
            userLatitude,
            userLongitude,
            station.getLatitude(),
            station.getLongitude()
        );

        if (distance > MAX_DISTANCE_METERS) {
            throw new RuntimeException(
                String.format("You are too far from the charging station. Current distance: %.0f meters. " +
                    "You must be within %.0f meters to start charging.", distance, MAX_DISTANCE_METERS)
            );
        }
        // ===== KẾT THÚC KIỂM TRA KHOẢNG CÁCH =====

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        // Kiểm tra connector type khớp hay không
        if (vehicle.getCarModel() == null || vehicle.getCarModel().getConnectorTypes() == null) {
            throw new RuntimeException("Vehicle car model or connector types not found");
        }

        if (!vehicle.getCarModel().getConnectorTypes().contains(point.getConnectorType())) {
            throw new RuntimeException("Vehicle connector type mismatch");
        }

        LocalDateTime startTime = LocalDateTime.now();

        // Cập nhật status charging point
        point.setStatus(ChargingPoint.ChargingPointStatus.OCCUPIED);
        chargingPointRepository.save(point);

        // Tạo session
        Session session = new Session();
        session.setOrder(order);
        session.setStartTime(startTime);
        session.setStatus(Session.SessionStatus.CHARGING);
        session.setBaseCost(0.0);
        session.setPowerConsumed(0.0);
        order.setStatus(Order.Status.CHARGING);
        orderRepository.save(order);
        session = sessionRepository.save(session);

        // Gửi notification
        notificationService.createBookingOrderNotification(orderId, NotificationServiceImpl.NotificationEvent.SESSION_START, null);

        // Push initial session progress via WebSocket (0 power, 0 cost)
        try {
            long estimatedTotalMinutes = expectedMinutes(vehicle, order.getExpectedBattery());
            double startBattery = order.getStartedBattery();
            double targetBattery = order.getExpectedBattery();

            SessionProgressDTO dto = new SessionProgressDTO(
                startBattery,       // startBattery
                startBattery,       // currentBattery (same as start initially)
                targetBattery,      // targetBattery
                0.0,                // progressPercentage (0% at start)
                0.0,                // powerConsumed
                0.0,                // cost
                0L,                 // elapsedSeconds
                0L,                 // elapsedMinutes
                estimatedTotalMinutes, // estimatedRemainingMinutes
                startTime,          // startTime
                startTime,          // currentTime (just started)
                "CHARGING"          // status
            );
            // sessionWebSocketService.sendSessionProgressToUser(order.getUser(), dto);
        } catch (Exception ignored) {}

        return session.getSessionId();
    }


    // US11: Giám sát phiên sạc (Giả sử poll-based, cập nhật mỗi GET)
    @Override
    public SessionProgressDTO monitorSession(Long sessionId, Long userId) {

        //.1> Validate
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Kiểm tra ownership
        if (!session.getOrder().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to monitor this session");
        }

        // Cho phép monitor cả CHARGING và PARKING (đã đạt target, đang đỗ xe)
        if (session.getStatus() != Session.SessionStatus.CHARGING &&
            session.getStatus() != Session.SessionStatus.PARKING) {
            throw new RuntimeException("Session not active or completed");
        }

        ConnectorType connectorType = session.getOrder().getChargingPoint().getConnectorType();
        // 2. Tính toán tiến trình sạc (chuẩn bị data trả về DTO)
        Vehicle vehicle = vehicleRepository.findById(session.getOrder().getVehicle().getId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found for session"));

        double power = connectorType.getPowerOutput(); // kW
        LocalDateTime now = LocalDateTime.now();
        
        // Nếu session đã dừng (PARKING), sử dụng endTime thay vì now
        LocalDateTime effectiveEndTime = (session.getStatus() == Session.SessionStatus.PARKING && session.getEndTime() != null)
            ? session.getEndTime()
            : now;

        // Tính toán chính xác theo giây thay vì phút để tránh làm tròn
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), effectiveEndTime);
        long minutesElapsed = secondsElapsed / 60; // Tính phút từ giây

        // Tính powerConsumed chính xác từ giây
        double powerConsumed = power * (secondsElapsed / 3600.0); // kWh = kW * (seconds / 3600)

        double basePrice = connectorType.getPricePerKWh();
        double priceFactor = 1.0;
        double discount = 0.0;
        double cost = powerConsumed * basePrice * priceFactor * (1 - discount);

        // Chỉ cập nhật nếu đang CHARGING
        if (session.getStatus() == Session.SessionStatus.CHARGING) {
            session.setPowerConsumed(powerConsumed);
            session.setBaseCost(cost);

            // Kiểm tra nếu đạt expectedBattery
            double currentBattery = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();

            log.info("Session {} - Battery check: current={}%, target={}%, powerConsumed={} kWh",
                     sessionId, String.format("%.2f", currentBattery),
                     String.format("%.2f", session.getOrder().getExpectedBattery()),
                     String.format("%.4f", powerConsumed));

            if (currentBattery >= session.getOrder().getExpectedBattery()) {
                if (currentBattery > 100) currentBattery = 100.0; // Chỉnh lại nếu vượt

                log.info("TARGET REACHED - Session {} transitioning to PARKING (current={}%, target={}%)",
                         sessionId, String.format("%.2f", currentBattery),
                         String.format("%.2f", session.getOrder().getExpectedBattery()));

                try {
                    // TỰ ĐỘNG DỪNG SẠC VÀ CHUYỂN SANG PARKING KHI ĐẠT TARGET
                    session.setEndTime(now);
                    session.setStatus(Session.SessionStatus.PARKING);
                    session.setParkingStartTime(now); // Bắt đầu tính grace period 15 phút
                    session.setPowerConsumed(powerConsumed);
                    session.setBaseCost(cost);

                    // Cập nhật order status
                    Order order = session.getOrder();
                    order.setStatus(Order.Status.COMPLETED);
                    Order savedOrder = orderRepository.save(order);

                    log.info("Order {} saved with status: {}", savedOrder.getOrderId(), savedOrder.getStatus());

                    // Gửi thông báo lần đầu khi đạt target
                    if (!session.getTargetReachedNotificationSent()) {
                        notificationService.createBookingOrderNotification(order.getOrderId(),
                            NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE,
                            "Đã sạc đến mức mong muốn. Vui lòng xác nhận rời trạm trong vòng 15 phút để tránh phí đỗ xe");
                        session.setTargetReachedNotificationSent(true);
                    }

                    Session savedSession = sessionRepository.save(session);

                    log.info("Session {} saved with status: {} (parking started at: {})",
                             savedSession.getSessionId(),
                             savedSession.getStatus(),
                             savedSession.getParkingStartTime());

                    // Verify thực sự đã save vào DB
                    Order verifiedOrder = orderRepository.findById(order.getOrderId()).orElse(null);
                    if (verifiedOrder != null && verifiedOrder.getStatus() != Order.Status.COMPLETED) {
                        log.error("CRITICAL: Order {} NOT COMPLETED in DB! Status is: {}",
                                  order.getOrderId(), verifiedOrder.getStatus());
                    }

                } catch (Exception e) {
                    log.error("Failed to transition session {} to PARKING: {}",
                              sessionId, e.getMessage(), e);
                    throw e;
                }
            } else {
                sessionRepository.save(session); // Cập nhật progress
            }
        }

        // Trả về DTO với dữ liệu hiện tại
        double startBattery = session.getOrder().getStartedBattery();
        double targetBattery = session.getOrder().getExpectedBattery();
        double currentBattery = calculateBatteryPercentage(vehicle, powerConsumed) + startBattery;
        if (currentBattery > 100) currentBattery = 100.0;

        // Calculate progress percentage: (current - start) / (target - start) * 100
        double progressPercentage = 0.0;
        if (targetBattery > startBattery) {
            progressPercentage = ((currentBattery - startBattery) / (targetBattery - startBattery)) * 100.0;
            progressPercentage = Math.min(100.0, Math.max(0.0, progressPercentage)); // Clamp 0-100
        }

        long estimatedTotalMinutes = expectedMinutes(vehicle, targetBattery);
        long remainingMinutes = estimatedTotalMinutes > minutesElapsed ? estimatedTotalMinutes - minutesElapsed : 0L;

        return new SessionProgressDTO(
            startBattery,        // startBattery
            currentBattery,      // currentBattery
            targetBattery,       // targetBattery
            progressPercentage,  // progressPercentage
            powerConsumed,       // powerConsumed
            cost,                // cost
            secondsElapsed,      // elapsedSeconds
            minutesElapsed,      // elapsedMinutes (backward compatibility)
            remainingMinutes,    // estimatedRemainingMinutes
            session.getStartTime(), // startTime
            effectiveEndTime,    // currentTime
            session.getStatus().name() // status (CHARGING or PARKING)
        );
    }

    @Override
    @Transactional
    public Long endSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        // Kiểm tra ownership
        if (!session.getOrder().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to end this session");
        }

        // Cho phép end session từ cả CHARGING và PARKING
        if (session.getStatus() != Session.SessionStatus.CHARGING &&
            session.getStatus() != Session.SessionStatus.PARKING) {
            throw new RuntimeException("Session cannot be ended from current status");
        }

        // ===== CASE 1: Nếu đang CHARGING, chuyển sang PARKING trước =====
        if (session.getStatus() == Session.SessionStatus.CHARGING) {
            transitionSessionToParking(session, "Dừng thủ công bởi người dùng");
            // Sau khi chuyển sang PARKING, return sessionId
            // User sẽ phải gọi endSession lần nữa để xác nhận rời đi
            return session.getSessionId();
        }

        // ===== CASE 2: Nếu đang PARKING, xử lý phí parking và hoàn thành session =====

        LocalDateTime now = LocalDateTime.now();

        // XỬ LÝ PHÍ PARKING (nếu đang ở trạng thái PARKING)
        if (session.getStatus() == Session.SessionStatus.PARKING && session.getParkingStartTime() != null) {
            // Tính thời gian từ khi bắt đầu parking đến khi user xác nhận rời đi
            long minutesSinceParkingStart = ChronoUnit.MINUTES.between(session.getParkingStartTime(), now);

            // Nếu quá 15 phút grace period → tính phí parking
            if (minutesSinceParkingStart > 15) {
                long chargeableMinutes = minutesSinceParkingStart - 15; // Trừ đi 15 phút grace

                // Sử dụng FeeCalculationService để tạo parking fee
                Fee fee = feeCalculationService.createParkingFee(session, chargeableMinutes);

                if (fee != null) {
                    // Gửi notification về phí đỗ xe
                    notificationService.createPenaltyNotification(session.getOrder().getOrderId(),
                            NotificationServiceImpl.PenaltyEvent.PARKING_PENALTY,
                            fee.getAmount(),
                            String.format("Đỗ xe %d phút sau grace period", chargeableMinutes));
                }
            }
            // Nếu <= 15 phút: không tính phí, user rời đi đúng thời gian
        }

        // Hoàn thành session
        session.setStatus(Session.SessionStatus.COMPLETED);

        // Update order status
        Order order = session.getOrder();
        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        // Giải phóng charging point
        ChargingPoint chargingPoint = order.getChargingPoint();
        if (chargingPoint != null) {
            chargingPoint.setStatus(ChargingPoint.ChargingPointStatus.AVAILABLE);
            chargingPointRepository.save(chargingPoint);
        }

        // Save session
        session = sessionRepository.save(session);

        // Gửi notification xác nhận hoàn thành (nếu chưa gửi)
        if (!session.getTargetReachedNotificationSent()) {
            notificationService.createBookingOrderNotification(order.getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE,
                "Cảm ơn bạn đã sử dụng dịch vụ!");
        }

        return session.getSessionId();
    }

    private void applyPenalty(Order order, Fee.Type type) { //Áp dụng phạt
        Fee fee = new Fee();
        fee.setOrder(order);
        fee.setType(type);
        fee.setAmount(calculatePenaltyAmount(type.toString(), order));
        fee.setAmount(fee.getAmount());
        fee.setIsPaid(false);
        fee.setCreatedAt(LocalDateTime.now());
        feeRepository.save(fee);

        // Gửi penalty notification
        NotificationServiceImpl.PenaltyEvent penaltyEvent;
        switch (type) {
            case NO_SHOW:
                penaltyEvent = NotificationServiceImpl.PenaltyEvent.NO_SHOW_PENALTY;
                break;
            case CANCEL:
                penaltyEvent = NotificationServiceImpl.PenaltyEvent.CANCEL_PENALTY;
                break;
            case PARKING:
                penaltyEvent = NotificationServiceImpl.PenaltyEvent.PARKING_PENALTY;
                break;
            default:
                penaltyEvent = NotificationServiceImpl.PenaltyEvent.NO_SHOW_PENALTY;
        }
        notificationService.createPenaltyNotification(order.getOrderId(), penaltyEvent, fee.getAmount(), "Tự động áp dụng phạt");
    }

    @Override //US12
    public Double calculatePenaltyAmount(String type, Order order) {
        // Implement based on BR12 examples
        return 0.0; // Placeholder
    }

    @Override //US11
    public Double calculateBatteryPercentage(Vehicle vehicle, Double kwh) {
        return (kwh / vehicle.getCarModel().getCapacity()) * 100;
    }

    @Override //US11
    public long expectedMinutes(Vehicle vehicle, Double expectedBattery) {
        // Calculate based on power and capacity
        return 0;
    }

    @Override
    public List<SessionDTO> getAllSessions() {
        return sessionRepository.findAll().stream().map(s -> {
            SessionDTO dto = new SessionDTO();
            dto.setSessionId(s.getSessionId());
            dto.setStatus(s.getStatus());
            dto.setParkingStartTime(s.getParkingStartTime());
            if (s.getOrder() != null) {
                dto.setOrderId(s.getOrder().getOrderId());
                var cp = s.getOrder().getChargingPoint();
                if (cp != null) dto.setChargingPointId(cp.getChargingPointId());
                dto.setVehicleId(s.getOrder().getVehicle() != null ? s.getOrder().getVehicle().getId() : null);
                dto.setStartTime(s.getStartTime());
                dto.setEndTime(s.getEndTime());
                dto.setCurrentBattery(s.getPowerConsumed());
                dto.setExpectedBattery(s.getOrder() != null ? s.getOrder().getExpectedBattery() : 0.0);
                dto.setConnectorTypeId(s.getOrder().getChargingPoint() != null && s.getOrder().getChargingPoint().getConnectorType() != null ?
                        s.getOrder().getChargingPoint().getConnectorType().getConnectorTypeId() : null);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public SessionDTO getSessionDetails(Long sessionId) {
        Session s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        SessionDTO dto = new SessionDTO();
        dto.setSessionId(s.getSessionId());
        dto.setStatus(s.getStatus());
        dto.setParkingStartTime(s.getParkingStartTime());
        if (s.getOrder() != null) {
            dto.setOrderId(s.getOrder().getOrderId());
            var cp = s.getOrder().getChargingPoint();
            if (cp != null) dto.setChargingPointId(cp.getChargingPointId());
            dto.setVehicleId(s.getOrder().getVehicle() != null ? s.getOrder().getVehicle().getId() : null);
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setCurrentBattery(s.getPowerConsumed());
            dto.setExpectedBattery(s.getOrder() != null ? s.getOrder().getExpectedBattery() : 0.0);
            dto.setConnectorTypeId(s.getOrder().getChargingPoint() != null && s.getOrder().getChargingPoint().getConnectorType() != null ?
                    s.getOrder().getChargingPoint().getConnectorType().getConnectorTypeId() : null);
        }
        return dto;
    }

    @Override
    public SessionDTO getSessionByOrderId(Long orderId) {
        Session s = sessionRepository.findByOrderOrderId(orderId);
        if (s == null) {
            return null;
        }
        SessionDTO dto = new SessionDTO();
        dto.setSessionId(s.getSessionId());
        dto.setStatus(s.getStatus());
        dto.setParkingStartTime(s.getParkingStartTime());
        if (s.getOrder() != null) {
            dto.setOrderId(s.getOrder().getOrderId());
            var cp = s.getOrder().getChargingPoint();
            if (cp != null) dto.setChargingPointId(cp.getChargingPointId());
            dto.setVehicleId(s.getOrder().getVehicle() != null ? s.getOrder().getVehicle().getId() : null);
            dto.setStartTime(s.getStartTime());
            dto.setEndTime(s.getEndTime());
            dto.setCurrentBattery(s.getPowerConsumed());
            dto.setExpectedBattery(s.getOrder() != null ? s.getOrder().getExpectedBattery() : 0.0);
            dto.setConnectorTypeId(s.getOrder().getChargingPoint() != null && s.getOrder().getChargingPoint().getConnectorType() != null ?
                    s.getOrder().getChargingPoint().getConnectorType().getConnectorTypeId() : null);
        }
        return dto;
    }

    @Override
    @Transactional
    public Long forceEndSession(Long sessionId, Long operatorId) {
        // Similar to endSession but skip ownership check; operatorId can be used for audit later
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != Session.SessionStatus.CHARGING &&
            session.getStatus() != Session.SessionStatus.PARKING) {
            throw new RuntimeException("Session not active");
        }

        // ===== CASE 1: Nếu đang CHARGING, chuyển sang PARKING =====
        if (session.getStatus() == Session.SessionStatus.CHARGING) {
            transitionSessionToParking(session, "Dừng bởi staff/operator");
            return session.getSessionId();
        }

        // ===== CASE 2: Nếu đang PARKING, force complete luôn =====
        LocalDateTime now = LocalDateTime.now();

        // XỬ LÝ PHÍ PARKING (nếu đang ở trạng thái PARKING)
        if (session.getStatus() == Session.SessionStatus.PARKING && session.getParkingStartTime() != null) {
            // Tính thời gian từ khi bắt đầu parking đến khi staff force complete
            long minutesSinceParkingStart = ChronoUnit.MINUTES.between(session.getParkingStartTime(), now);

            // Nếu quá 15 phút grace period → tính phí parking
            if (minutesSinceParkingStart > 15) {
                long chargeableMinutes = minutesSinceParkingStart - 15; // Trừ đi 15 phút grace

                // Sử dụng FeeCalculationService để tạo parking fee
                Fee fee = feeCalculationService.createParkingFee(session, chargeableMinutes);

                if (fee != null) {
                    // Override description cho staff force complete
                    fee.setDescription(String.format("Phí đỗ xe %d phút (staff force complete)", chargeableMinutes));
                    feeRepository.save(fee);

                    // Gửi notification về phí đỗ xe
                    notificationService.createPenaltyNotification(session.getOrder().getOrderId(),
                            NotificationServiceImpl.PenaltyEvent.PARKING_PENALTY,
                            fee.getAmount(),
                            String.format("Staff force complete - Đỗ xe %d phút sau grace period", chargeableMinutes));
                }
            }
        }

        // Hoàn thành session
        session.setStatus(Session.SessionStatus.COMPLETED);

        // Update order status
        Order order = session.getOrder();
        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        // Update charging point status
        ChargingPoint chargingPoint = order.getChargingPoint();
        if (chargingPoint != null) {
            chargingPoint.setStatus(ChargingPoint.ChargingPointStatus.AVAILABLE);
            chargingPointRepository.save(chargingPoint);
        }

        // Save session
        session = sessionRepository.save(session);

        // Send completion notification
        notificationService.createBookingOrderNotification(order.getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE, null);


        return session.getSessionId();
    }

    /**
     * DEPRECATED: Staff-related methods are no longer used in the new self-service flow
     * User now confirms departure themselves via endSession()
     */

    /*
    @Transactional
    public Long staffMarkStillParked(Long sessionId, Long staffId) {
        // No longer used - parking status is managed automatically
        throw new RuntimeException("This method is deprecated. Use self-service flow.");
    }

    @Transactional
    public Long staffMarkLeft(Long sessionId, Long staffId) {
        // No longer used - user confirms departure via endSession()
        throw new RuntimeException("This method is deprecated. Use endSession() instead.");
    }
    */

    /**
     * Helper method: Chuyển session sang trạng thái PARKING
     * Được gọi từ cả manual stop, auto-stop và force stop
     * @param session Session cần chuyển sang PARKING
     * @param stopReason Lý do dừng (để ghi log/notification)
     */
    private void transitionSessionToParking(Session session, String stopReason) {
        LocalDateTime now = LocalDateTime.now();

        // Nếu đã ở PARKING hoặc COMPLETED, bỏ qua
        if (session.getStatus() == Session.SessionStatus.PARKING ||
            session.getStatus() == Session.SessionStatus.COMPLETED) {
            return;
        }

        // 1. Tính toán và cập nhật charging cost (nếu chưa có endTime)
        if (session.getEndTime() == null) {
            session.setEndTime(now);
        }

        ConnectorType connectorType = session.getOrder().getChargingPoint().getConnectorType();
        double power = connectorType.getPowerOutput();
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), session.getEndTime());
        double powerConsumed = power * (secondsElapsed / 3600.0);
        double basePrice = connectorType.getPricePerKWh();
        double cost = powerConsumed * basePrice;

        session.setPowerConsumed(powerConsumed);
        session.setBaseCost(cost);

        // 2. Chuyển sang PARKING và bắt đầu grace period 15 phút
        session.setStatus(Session.SessionStatus.PARKING);
        session.setParkingStartTime(now);

        // 3. Cập nhật order status
        Order order = session.getOrder();
        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        // 4. Lưu session
        sessionRepository.save(session);

        // 5. Gửi notification cho user
        String message = String.format("Phiên sạc đã dừng (%s). Vui lòng xác nhận rời trạm trong vòng 15 phút để tránh phí đỗ xe.", stopReason);
        if (!session.getTargetReachedNotificationSent()) {
            notificationService.createBookingOrderNotification(
                order.getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE,
                message
            );
            session.setTargetReachedNotificationSent(true);
            sessionRepository.save(session);
        }
    }
}
