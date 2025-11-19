package swp391.code.swp391.service;

import java.math.BigDecimal;
import java.util.Map;

public interface VNPayService {

    /**
     * Tạo URL thanh toán VNPay
     * @param transactionId ID giao dịch
     * @param amount Số tiền thanh toán
     * @param orderInfo Thông tin đơn hàng
     * @param returnUrl URL để quay về sau khi thanh toán
     * @param bankCode Mã ngân hàng (tùy chọn)
     * @return URL thanh toán VNPay
     */
    String createPaymentUrl(Long transactionId, BigDecimal amount, String orderInfo,
                            String returnUrl, String bankCode);

    /**
     * Xử lý callback từ VNPay sau khi thanh toán
     * @param params Các tham số trả về từ VNPay
     * @return true nếu thanh toán thành công, false nếu thất bại
     */
    boolean handlePaymentCallback(Map<String, String> params);

    /**
     * Xác thực chữ ký từ VNPay
     * @param params Các tham số từ VNPay
     * @return true nếu chữ ký hợp lệ, false nếu không
     */
    boolean verifyPaymentSignature(Map<String, String> params);

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay
     * @param params Các tham số từ VNPay IPN
     * @return Response code theo chuẩn VNPay
     */
    String handleIPN(Map<String, String> params);
}