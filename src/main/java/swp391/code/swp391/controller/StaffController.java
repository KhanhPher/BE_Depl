package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.*;
import swp391.code.swp391.service.StaffService;
import swp391.code.swp391.util.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
//@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    private final StaffService staffService;
    private final JwtUtil jwtUtil;
    /**
     * API đổi trụ sạc cho driver
     * Endpoint: POST /api/staff/change-charging-point
     */
    @PostMapping("/change-charging-point")
    public ResponseEntity<APIResponse<ChangeChargingPointResponseDTO>> changeChargingPoint(
            @Valid @RequestBody ChangeChargingPointRequestDTO request) {

        try {
            ChangeChargingPointResponseDTO response = staffService.changeChargingPointForDriver(request);

            return ResponseEntity.ok(
                    APIResponse.<ChangeChargingPointResponseDTO>builder()
                            .success(true)
                            .message("Đổi trụ sạc thành công!")
                            .data(response)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<ChangeChargingPointResponseDTO>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.<ChangeChargingPointResponseDTO>builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * API tìm trụ sạc thay thế
     * Endpoint: GET /api/staff/find-alternative-points
     */
    @GetMapping("/find-alternative-points")
    public ResponseEntity<APIResponse<List<ChargingPointDTO>>> findAlternativePoints(
            @RequestParam Long orderId,
            @RequestParam Long currentChargingPointId) {

        try {
            List<ChargingPointDTO> alternatives = staffService.findAlternativeChargingPoints(
                    orderId, currentChargingPointId);

            return ResponseEntity.ok(
                    APIResponse.<List<ChargingPointDTO>>builder()
                            .success(true)
                            .message("Tìm thấy " + alternatives.size() + " trụ sạc thay thế")
                            .data(alternatives)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<List<ChargingPointDTO>>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.<List<ChargingPointDTO>>builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
    /**
     * Xem các order bị conflict thời gian
     */
    @GetMapping("/station/{stationId}/conflicts")
    public ResponseEntity<APIResponse<StationConflictResponseDTO>> getStationConflicts(
            @PathVariable Long stationId) {

        try {
            StationConflictResponseDTO conflicts = staffService.getConflictingOrdersByStation(stationId);

            String message = conflicts.getTotalConflicts() > 0
                    ? String.format("Tìm thấy %d conflicts tại trạm %s",
                    conflicts.getTotalConflicts(), conflicts.getStationName())
                    : "Không có conflict nào tại trạm này";

            return ResponseEntity.ok(
                    APIResponse.<StationConflictResponseDTO>builder()
                            .success(true)
                            .message(message)
                            .data(conflicts)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<StationConflictResponseDTO>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * Lấy danh sách station mà staff quản lý
     */
    @GetMapping("/my-stations")
    public ResponseEntity<APIResponse<List<Long>>> getMyStations(@RequestParam Long staffId) {

        try {
            List<Long> stationIds = staffService.getStationsManagedByStaff(staffId);

            return ResponseEntity.ok(
                    APIResponse.<List<Long>>builder()
                            .success(true)
                            .message("Tìm thấy " + stationIds.size() + " trạm sạc")
                            .data(stationIds)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<List<Long>>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }


    }

    /**
     * Quản lí báo cáo sự cố (Staff tạo báo cáo)
     */
    @PostMapping("/issue-reports")
    public ResponseEntity<APIResponse<Long>> createIssueReport(@Valid @RequestBody IssueReportDTO issueReportDTO) {
        try {
            Long staffId = jwtUtil.getUserByTokenThroughSecurityContext().getUserId();
            Long issueId = staffService.createIssueReport(issueReportDTO, staffId);
            return ResponseEntity.ok(APIResponse.success("Issue report created successfully", issueId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("Failed to create issue report: " + e.getMessage()));
        }
    }

    /**
     * Xem danh sách phiên sạc của trạm
     * Endpoint: GET /api/staff/station/{stationId}/sessions
     */
    @GetMapping("/station/{stationId}/sessions")
    public ResponseEntity<APIResponse<List<SessionListDTO>>> getStationSessions(
            @PathVariable Long stationId) {
        try {
            List<SessionListDTO> sessions = staffService.getSessionsByStation(stationId);

            String message = sessions.isEmpty()
                    ? "Chưa có phiên sạc nào tại trạm này"
                    : String.format("Tìm thấy %d phiên sạc tại trạm này", sessions.size());

            return ResponseEntity.ok(
                    APIResponse.<List<SessionListDTO>>builder()
                            .success(true)
                            .message(message)
                            .data(sessions)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<List<SessionListDTO>>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.<List<SessionListDTO>>builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * Tạo phiên sạc ngay lập tức cho driver
     * Nếu không có slot trống, tự động gợi ý thời gian khả dụng
     * Endpoint: POST /api/staff/create-immediate-session
     */
    @PostMapping("/create-immediate-session")
    public ResponseEntity<APIResponse<?>> createImmediateSession(
            @Valid @RequestBody OrderRequestDTO request) {
        try {
            // Lấy staffId từ token
            Long staffId = jwtUtil.getUserByTokenThroughSecurityContext().getUserId();
            request.setStaffId(staffId);

            // Gọi service để tạo session ngay lập tức
            Object response = staffService.createImmediateSession(request);

            // Nếu là OrderResponseDTO => Thành công
            if (response instanceof OrderResponseDTO) {
                OrderResponseDTO orderResponse = (OrderResponseDTO) response;
                return ResponseEntity.ok(
                        APIResponse.builder()
                                .success(true)
                                .message("Đã tạo phiên sạc thành công! Order ID: " + orderResponse.getOrderId())
                                .data(orderResponse)
                                .build()
                );
            }
            // Nếu là AvailableSlotsResponseDTO => Không có slot ngay, trả về suggested slots
            else if (response instanceof AvailableSlotsResponseDTO) {
                AvailableSlotsResponseDTO suggestions = (AvailableSlotsResponseDTO) response;
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        APIResponse.builder()
                                .success(false)
                                .message("Không có slot trống ngay lập tức. Vui lòng xem gợi ý thời gian khả dụng.")
                                .data(suggestions)
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        APIResponse.builder()
                                .success(false)
                                .message("Lỗi không xác định")
                                .data(null)
                                .build()
                );
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * Xem danh sách trụ sạc của trạm
     * Endpoint: GET /api/staff/station/{stationId}/charging-points
     */
    @GetMapping("/station/{stationId}/charging-points")
    public ResponseEntity<APIResponse<List<ChargingPointDTO>>> getStationChargingPoints(
            @PathVariable Long stationId) {
        try {
            List<ChargingPointDTO> chargingPoints = staffService.getChargingPointsByStation(stationId);

            String message = chargingPoints.isEmpty()
                    ? "Không có trụ sạc nào tại trạm này"
                    : String.format("Tìm thấy %d trụ sạc tại trạm này", chargingPoints.size());

            return ResponseEntity.ok(
                    APIResponse.<List<ChargingPointDTO>>builder()
                            .success(true)
                            .message(message)
                            .data(chargingPoints)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<List<ChargingPointDTO>>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * Xem lịch sử giao dịch của trạm
     * Endpoint: GET /api/staff/station/{stationId}/transactions
     */
    @GetMapping("/station/{stationId}/transactions")
    public ResponseEntity<APIResponse<List<TransactionHistoryDTO>>> getStationTransactions(
            @PathVariable Long stationId) {
        try {
            List<TransactionHistoryDTO> transactions = staffService.getTransactionHistoryByStation(stationId);

            String message = transactions.isEmpty()
                    ? "Chưa có giao dịch nào tại trạm này"
                    : String.format("Tìm thấy %d giao dịch tại trạm này", transactions.size());

            return ResponseEntity.ok(
                    APIResponse.<List<TransactionHistoryDTO>>builder()
                            .success(true)
                            .message(message)
                            .data(transactions)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<List<TransactionHistoryDTO>>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    /**
     * Thanh toán tại chỗ cho driver (cash payment)
     * Endpoint: POST /api/staff/onsite-payment/{sessionId}
     */
    @PostMapping("/onsite-payment/{sessionId}")
    public ResponseEntity<APIResponse<TransactionHistoryDTO>> processOnsitePayment(
            @PathVariable Long sessionId) {
        try {
            // Lấy staffId từ token
            Long staffId = jwtUtil.getUserByTokenThroughSecurityContext().getUserId();

            TransactionHistoryDTO transaction = staffService.processOnsitePayment(sessionId, staffId);

            return ResponseEntity.ok(
                    APIResponse.<TransactionHistoryDTO>builder()
                            .success(true)
                            .message("Thanh toán tại chỗ thành công! Transaction ID: " + transaction.getTransactionId())
                            .data(transaction)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    APIResponse.<TransactionHistoryDTO>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    APIResponse.<TransactionHistoryDTO>builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}