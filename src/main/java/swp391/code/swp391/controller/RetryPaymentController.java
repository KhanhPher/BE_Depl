package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.RetryPaymentRequestDTO;
import swp391.code.swp391.dto.RetryPaymentResponseDTO;
import swp391.code.swp391.exception.ApiRequestException;
import swp391.code.swp391.service.PaymentService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RetryPaymentController {

    private final PaymentService paymentService;

    /**
     * API: Retry payment cho transaction FAILED
     * POST /api/payment/retry
     */
    @PostMapping("/retry")
    @PreAuthorize("hasAnyRole('DRIVER', 'STAFF')")
    public ResponseEntity<Map<String, Object>> retryPayment(
            @Valid @RequestBody RetryPaymentRequestDTO request) {

        log.info("Retry payment - TransactionId: {}, UserId: {}",
                request.getTransactionId(), request.getUserId());

        Map<String, Object> response = new HashMap<>();

        try {
            RetryPaymentResponseDTO result = paymentService.retryPayment(request);

            response.put("success", true);
            response.put("message", "Tạo yêu cầu thanh toán lại thành công");
            response.put("data", result);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (ApiRequestException e) {
            log.error("Retry payment failed: {}", e.getMessage());

            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("data", null);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());

            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            response.put("data", null);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}