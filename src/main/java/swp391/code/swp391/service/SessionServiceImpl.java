package swp391.code.swp391.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.SessionProgressDTO;
import swp391.code.swp391.dto.SessionDTO;
import swp391.code.swp391.entity.*;
import swp391.code.swp391.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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
            SessionProgressDTO dto = new SessionProgressDTO(
                order.getStartedBattery(), // current battery = started battery
                0.0, // power consumed
                0.0, // cost
                0L, // elapsed seconds
                0L, // elapsed minutes
                estimatedTotalMinutes, // estimated remaining
                startTime, // start time
                startTime // current time (just started)
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

        if (session.getStatus() != Session.SessionStatus.CHARGING) {
            throw new RuntimeException("Session not active");
        }

        ConnectorType connectorType = session.getOrder().getChargingPoint().getConnectorType();
        // 2. Tính toán tiến trình sạc (chuẩn bị data trả về DTO)
        Vehicle vehicle = vehicleRepository.findById(session.getOrder().getVehicle().getId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found for session"));

        double power = connectorType.getPowerOutput(); // kW
        LocalDateTime now = LocalDateTime.now();
        
        // Tính toán chính xác theo giây thay vì phút để tránh làm tròn
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), now);
        long minutesElapsed = secondsElapsed / 60; // Tính phút từ giây

        // Tính powerConsumed chính xác từ giây
        double powerConsumed = power * (secondsElapsed / 3600.0); // kWh = kW * (seconds / 3600)

        double basePrice = connectorType.getPricePerKWh();
        double priceFactor = 1.0;
        double discount = 0.0;
        double cost = powerConsumed * basePrice * priceFactor * (1 - discount);
        //===============================================================
        session.setPowerConsumed(powerConsumed);
        session.setBaseCost(cost);

        // Kiểm tra nếu đạt expectedBattery
        double currentBattery = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();
        if (currentBattery >= session.getOrder().getExpectedBattery()) {
            if (currentBattery > 100) currentBattery = 100.0; // Chỉnh lại nếu vượt
            // Gui thong bao o day

            // Nếu tiếp tục sau đầy pin, áp phạt (giả sử check sau)
            if (minutesElapsed > expectedMinutes(vehicle, session.getOrder().getExpectedBattery())) {
                applyPenalty(session.getOrder(), Fee.Type.CHARGING);
            }
        } else {
            sessionRepository.save(session); // Cập nhật progress
        }

        // Trả về DTO
        long estimatedTotalMinutes = expectedMinutes(vehicle, session.getOrder().getExpectedBattery());
        long remainingMinutes = estimatedTotalMinutes > minutesElapsed ? estimatedTotalMinutes - minutesElapsed : 0L;

        SessionProgressDTO dto = new SessionProgressDTO(
            currentBattery,
            powerConsumed,
            cost,
            secondsElapsed, // elapsed seconds (CHÍNH XÁC)
            minutesElapsed, // elapsed minutes (để backward compatibility)
            remainingMinutes, // estimated remaining
            session.getStartTime(), // start time
            now // current time
        );

        return dto;
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

        if (session.getStatus() != Session.SessionStatus.CHARGING) {
            throw new RuntimeException("Session not active");
        }

        ConnectorType connectorType = session.getOrder().getChargingPoint().getConnectorType();
        Vehicle vehicle = vehicleRepository.findById(session.getOrder().getVehicle().getId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found for session"));

        LocalDateTime now = LocalDateTime.now();
        double power = connectorType.getPowerOutput();
        
        // Tính toán chính xác theo giây thay vì phút để tránh làm tròn
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), now);
        long minutesElapsed = secondsElapsed / 60; // Tính phút từ giây
        double powerConsumed = power * (secondsElapsed / 3600.0); // kWh = kW * (seconds / 3600)

        // Calculate cost
        double basePrice = connectorType.getPricePerKWh();
        double priceFactor = 1.0;
        double discount = 0.0;
        double cost = powerConsumed * basePrice * priceFactor * (1 - discount);

        // Update session - KHÔNG hoàn tất ngay, chuyển sang chờ staff xác nhận
        session.setBaseCost(cost);
        session.setPowerConsumed(powerConsumed);
        session.setEndTime(now);
        session.setStatus(Session.SessionStatus.WAITING_STAFF_DECISION);

        // Update order status
        Order order = session.getOrder();
        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        // KHÔNG giải phóng charging point - chờ staff xác nhận
        // ChargingPoint vẫn ở trạng thái OCCUPIED

        // Calculate final battery percentage
        double finalBattery = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();
        if (finalBattery > 100) {
            finalBattery = 100.0;
        }

        // Check for overtime penalty
        if (minutesElapsed > expectedMinutes(vehicle, session.getOrder().getExpectedBattery())) {
            applyPenalty(order, Fee.Type.CHARGING);
        }

        // Save session
        session = sessionRepository.save(session);

        // Gửi notification cho USER để nhắc rời trạm
        notificationService.createBookingOrderNotification(order.getOrderId(),
            NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE,
            "Vui lòng rời khỏi trạm sạc để tránh phí đỗ xe");

        // Push final progress via WebSocket
        try {
            double finalBatteryPercent = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();
            if (finalBatteryPercent > 100) finalBatteryPercent = 100.0;

            SessionProgressDTO finalDto = new SessionProgressDTO(
                finalBatteryPercent,
                powerConsumed,
                session.getBaseCost(),
                secondsElapsed, // elapsed seconds (use variable from above)
                minutesElapsed, // elapsed minutes
                0L, // remaining = 0 (completed)
                session.getStartTime(), // start time
                now // current time
            );
            // sessionWebSocketService.sendSessionProgressToUser(order.getUser(), finalDto);
        } catch (Exception ignored) {}

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
            case CHARGING:
                penaltyEvent = NotificationServiceImpl.PenaltyEvent.OVERTIME_PENALTY;
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

        if (session.getStatus() != Session.SessionStatus.CHARGING) {
            throw new RuntimeException("Session not active");
        }

        ConnectorType connectorType = session.getOrder().getChargingPoint().getConnectorType();
        Vehicle vehicle = vehicleRepository.findById(session.getOrder().getVehicle().getId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found for session"));

        LocalDateTime now = LocalDateTime.now();
        double power = connectorType.getPowerOutput();
        
        // Tính toán chính xác theo giây thay vì phút để tránh làm tròn
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartTime(), now);
        long minutesElapsed = secondsElapsed / 60; // Tính phút từ giây
        double powerConsumed = power * (secondsElapsed / 3600.0); // kWh = kW * (seconds / 3600)

        // Calculate cost
        double basePrice = connectorType.getPricePerKWh();
        double priceFactor = 1.0;
        double discount = 0.0;
        double cost = powerConsumed * basePrice * priceFactor * (1 - discount);

        // Update session
        session.setBaseCost(cost);
        session.setPowerConsumed(powerConsumed);
        session.setEndTime(now);
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

        // Calculate final battery percentage
        double finalBattery = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();
        if (finalBattery > 100) {
            finalBattery = 100.0;
        }

        // Check for overtime penalty
        if (minutesElapsed > expectedMinutes(vehicle, session.getOrder().getExpectedBattery())) {
            applyPenalty(order, Fee.Type.CHARGING);
        }

        // Save session
        session = sessionRepository.save(session);

        // Send completion notification
        notificationService.createBookingOrderNotification(order.getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE, null);

        // Push final progress via WebSocket
        try {
            double finalBatteryPercent = calculateBatteryPercentage(vehicle, powerConsumed) + session.getOrder().getStartedBattery();
            if (finalBatteryPercent > 100) finalBatteryPercent = 100.0;

            SessionProgressDTO finalDto = new SessionProgressDTO(
                finalBatteryPercent,
                powerConsumed,
                session.getBaseCost(),
                secondsElapsed, // elapsed seconds (use variable from above)
                minutesElapsed, // elapsed minutes
                0L, // remaining = 0 (force completed)
                session.getStartTime(), // start time
                now // current time (end time)
            );
            // sessionWebSocketService.sendSessionProgressToUser(order.getUser(), finalDto);
        } catch (Exception ignored) {}

        return session.getSessionId();
    }

    /**
     * Staff xác nhận xe vẫn đậu tại trạm (slot vẫn bị chiếm dụng, bắt đầu tính phí đỗ xe)
     */
    @Transactional
    public Long staffMarkStillParked(Long sessionId, Long staffId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != Session.SessionStatus.WAITING_STAFF_DECISION) {
            throw new RuntimeException("Session not awaiting staff decision");
        }

        // Chuyển session sang trạng thái STILL_PARKED và ghi nhận thời điểm bắt đầu tính phí
        session.setStatus(Session.SessionStatus.STILL_PARKED);
        // Grace period 15 phút sau khi kết thúc phiên sạc: không tính phí trong khoảng này
        LocalDateTime graceStart = (session.getEndTime() != null)
                ? session.getEndTime().plusMinutes(15)
                : LocalDateTime.now().plusMinutes(15);
        session.setParkingStartTime(graceStart);
        sessionRepository.save(session);

        // Charging point vẫn giữ trạng thái OCCUPIED (không thay đổi)

        // Gửi notification cho người dùng
        notificationService.createBookingOrderNotification(session.getOrder().getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_STILL_PARKED, null);

        return session.getSessionId();
    }

    /**
     * Staff xác nhận xe đã rời trạm
     * Nếu trước đó đã mark STILL_PARKED thì tính phí đỗ xe
     */
    @Transactional
    public Long staffMarkLeft(Long sessionId, Long staffId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStatus() != Session.SessionStatus.WAITING_STAFF_DECISION
                && session.getStatus() != Session.SessionStatus.STILL_PARKED) {
            throw new RuntimeException("Session not awaiting staff decision or not in parked state");
        }

        // Nếu xe vẫn đậu, tính phí đỗ xe từ parkingStartTime đến hiện tại
        if (session.getStatus() == Session.SessionStatus.STILL_PARKED) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = session.getParkingStartTime() != null ?
                    session.getParkingStartTime() : session.getEndTime();
            long parkedMinutes = Math.max(0L, ChronoUnit.MINUTES.between(start, now));

            if (parkedMinutes > 0) {
                double parkingAmount = calculateParkingFee(session, parkedMinutes);

                // Tạo fee cho phí đỗ xe
                Fee fee = new Fee();
                fee.setOrder(session.getOrder());
                fee.setSession(session);
                fee.setType(Fee.Type.PARKING);
                fee.setAmount(parkingAmount);
                fee.setIsPaid(false);
                fee.setCreatedAt(LocalDateTime.now());
                fee.setDescription(String.format("Phí đỗ xe %d phút sau khi sạc xong", parkedMinutes));
                feeRepository.save(fee);

                // Gửi notification về phí đỗ xe
                notificationService.createPenaltyNotification(session.getOrder().getOrderId(),
                        NotificationServiceImpl.PenaltyEvent.PARKING_PENALTY,
                        fee.getAmount(),
                        String.format("Đỗ xe %d phút sau khi sạc xong", parkedMinutes));
            }
        }

        // Hoàn tất session và giải phóng charging point
        session.setStatus(Session.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        Order order = session.getOrder();
        // Order đã được set COMPLETED trong endSession, giữ nguyên
        order.setStatus(Order.Status.COMPLETED);
        orderRepository.save(order);

        ChargingPoint cp = order.getChargingPoint();
        if (cp != null) {
            cp.setStatus(ChargingPoint.ChargingPointStatus.AVAILABLE);
            chargingPointRepository.save(cp);
        }

        // Gửi notification hoàn tất
        notificationService.createBookingOrderNotification(order.getOrderId(),
                NotificationServiceImpl.NotificationEvent.SESSION_COMPLETE, null);

        return session.getSessionId();
    }

    /**
     * Tính phí đỗ xe theo thời gian (tăng dần theo công thức)
     * Công thức: baseRate * minutes * (1 + 0.5 * floor(hours))
     * - baseRate: 500 VND/phút (có thể điều chỉnh)
     * - Mỗi giờ tăng thêm 50% phí
     * - Phí tối thiểu: 10,000 VND
     */
    private double calculateParkingFee(Session session, long parkedMinutes) {
        if (parkedMinutes <= 0) return 0.0;

        double baseRatePerMinute = 500.0; // 500 VND/phút
        double hours = Math.floor(parkedMinutes / 60.0);
        double multiplier = 1.0 + (0.5 * hours); // +50% mỗi giờ
        double amount = baseRatePerMinute * parkedMinutes * multiplier;

        double minimum = 10000.0; // Phí tối thiểu 10,000 VND
        return Math.max(amount, minimum);
    }
}
