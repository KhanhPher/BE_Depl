package swp391.code.swp391.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.PaymentDetailDTO;
import swp391.code.swp391.dto.PaymentRequestDTO;
import swp391.code.swp391.dto.PaymentResponseDTO;
import swp391.code.swp391.dto.SubscriptionPaymentRequestDTO;
import swp391.code.swp391.service.PaymentService;
import swp391.code.swp391.service.VNPayService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayService vnPayService;

    /**
     * Tính toán số tiền thanh toán cho phiên sạc
     * GET /api/payment/calculate?sessionId={sessionId}&userId={userId}
     */
    @GetMapping("/calculate")
    public ResponseEntity<?> calculatePaymentAmount(
            @RequestParam Long sessionId,
            @RequestParam Long userId) {
        try {
            log.info("API: Tính toán số tiền thanh toán - Session: {}, User: {}", sessionId, userId);

            BigDecimal amount = paymentService.calculatePaymentAmount(sessionId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("amount", amount);
            response.put("message", "Tính toán thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi tính toán số tiền thanh toán: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    /**
     * Lấy chi tiết thanh toán trước khi thực hiện thanh toán
     * GET /api/payment/detail?sessionId={sessionId}&userId={userId}
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getPaymentDetail(
            @RequestParam Long sessionId,
            @RequestParam Long userId) {
        try {
            log.info("API: Lấy chi tiết thanh toán - Session: {}, User: {}", sessionId, userId);

            PaymentDetailDTO detail = paymentService.getPaymentDetail(sessionId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", detail);
            response.put("message", "Lấy thông tin thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết thanh toán: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Khởi tạo thanh toán
     * POST /api/payment/initiate
     * Body: PaymentRequest
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequestDTO request) {
        try {
            log.info("API: Khởi tạo thanh toán - Session: {}, User: {}, Method: {}",
                    request.getSessionId(), request.getUserId(), request.getPaymentMethod());

            PaymentResponseDTO response = paymentService.createPayment(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Khởi tạo thanh toán thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo thanh toán: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Thanh toán bằng tiền mặt
     * POST /api/payment/cash
     */
    @PostMapping("/cash")
    public ResponseEntity<?> processCashPayment(
            @RequestParam Long sessionId,
            @RequestParam Long userId) {
        try {
            log.info("API: Thanh toán tiền mặt - Session: {}, User: {}", sessionId, userId);

            PaymentResponseDTO response = paymentService.processCashPayment(sessionId, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Thanh toán tiền mặt thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi thanh toán tiền mặt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Callback từ VNPay sau khi thanh toán
     * GET /api/payment/vnpay/callback
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> vnpayCallback(HttpServletRequest request) {
        try {
            log.info("API: Nhận callback từ VNPay");

            // Lấy tất cả các tham số từ request
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });

            // Xử lý callback
            boolean success = vnPayService.handlePaymentCallback(params);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);

            if (success) {
                response.put("message", "Thanh toán VNPay thành công");
                response.put("transactionId", params.get("vnp_TxnRef"));
            } else {
                response.put("message", "Thanh toán VNPay thất bại");
                response.put("errorCode", params.get("vnp_ResponseCode"));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý callback VNPay: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * IPN (Instant Payment Notification) từ VNPay
     * GET /api/payment/vnpay/ipn
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<String> vnpayIPN(HttpServletRequest request) {
        try {
            log.info("API: Nhận IPN từ VNPay");

            // Lấy tất cả các tham số từ request
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });

            // Xử lý IPN
            String response = vnPayService.handleIPN(params);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(response);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý IPN VNPay: {}", e.getMessage());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body("{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}");
        }
    }

    /**
     * Gửi lại hóa đơn qua email
     * POST /api/payment/resend-invoice/{transactionId}
     */
    @PostMapping("/resend-invoice/{transactionId}")
    public ResponseEntity<?> resendInvoice(@PathVariable Long transactionId) {
        try {
            log.info("API: Gửi lại hóa đơn cho transaction: {}", transactionId);

            paymentService.sendInvoiceEmail(transactionId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi lại hóa đơn thành công"
            ));
        } catch (Exception e) {
            log.error("Lỗi khi gửi lại hóa đơn: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable Long transactionId) {
        try {
            log.info("API: Lấy thông tin giao dịch: {}", transactionId);

            var transaction = paymentService.getTransaction(transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transaction);
            response.put("message", "Lấy thông tin giao dịch thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin giao dịch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    @PostMapping("/subscription")
    public ResponseEntity<?> payForSubscription(@Valid @RequestBody SubscriptionPaymentRequestDTO request) {
        try {
            PaymentResponseDTO response = paymentService.payForSubscription(
                    request.getUserId(),
                    request.getSubscriptionId(),
                    request.getReturnUrl(),
                    request.getBankCode()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Khởi tạo thanh toán subscription thành công");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi thanh toán subscription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }
}