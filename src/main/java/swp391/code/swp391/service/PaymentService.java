package swp391.code.swp391.service;

import swp391.code.swp391.dto.*;
import swp391.code.swp391.entity.Transaction;

import java.math.BigDecimal;

public interface PaymentService {

    /**
     * Tính tổng số tiền thanh toán cho một phiên sạc
     * Công thức: baseCost + totalFees
     * Trong đó baseCost = powerConsumed × basePrice × priceFactor × (1 - subscriptionDiscount)
     */
    BigDecimal calculatePaymentAmount(Long sessionId, Long userId);

    /**
     * Lấy chi tiết thanh toán để hiển thị trước khi thanh toán
     */
    PaymentDetailDTO getPaymentDetail(Long sessionId, Long userId);

    /**
     * Tạo yêu cầu thanh toán mới
     * - Với CASH: xử lý thanh toán trực tiếp
     * - Với VNPAY: trả về URL thanh toán
     */
    PaymentResponseDTO createPayment(PaymentRequestDTO request);

    /**
     * Xử lý thanh toán bằng tiền mặt
     */
    PaymentResponseDTO processCashPayment(Long sessionId, Long userId);

    /**
     * Hoàn tất thanh toán sau khi giao dịch thành công
     */
    void completePayment(Long transactionId);

    /**
     * Xử lý thanh toán thất bại
     */
    void handleFailedPayment(Long transactionId, String reason);

    /**
     * Gửi hóa đơn qua email sau khi thanh toán thành công
     */
    void sendInvoiceEmail(Long transactionId);

    /**
     * Lấy thông tin giao dịch theo ID
     */
    Transaction getTransaction(Long transactionId);

    /**
     * RETRY PAYMENT - Thanh toán lại cho transaction FAILED
     * - Validate transaction phải có status FAILED
     * - Validate user ownership
     * - Hỗ trợ cả VNPAY và CASH
     * - Tự động unlock account nếu thanh toán hết phí
     */
    RetryPaymentResponseDTO retryPayment(RetryPaymentRequestDTO request);

    /**
     * Thanh toán cho subscription (CHỈ VNPAY)
     * - Tạo transaction cho subscription payment
     * - Tạo URL thanh toán VNPay
     * - Sau khi thanh toán thành công (callback): Cập nhật subscription của user
     * - Gửi notification
     */
    PaymentResponseDTO payForSubscription(Long userId, Long subscriptionId, String returnUrl, String bankCode);

    /**
     * Hoàn tất thanh toán subscription khi VNPay trả về thành công.
     * Lưu lại thông tin giao dịch từ VNPay và cập nhật subscription cho user.
     */
    void completeSubscriptionPayment(Long transactionId, String vnpTransactionNo, String vnpBankCode, String vnpCardType);

    /**
     * Xử lý thất bại cho giao dịch subscription.
     */
    void handleFailedSubscriptionPayment(Long transactionId, String reason);
}