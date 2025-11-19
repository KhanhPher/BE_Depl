package swp391.code.swp391.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.entity.Transaction;
import swp391.code.swp391.repository.TransactionRepository;
import swp391.code.swp391.config.VNPayConfig;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPayServiceImpl implements VNPayService {

    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService; // Đổi từ final

    // THÊM CONSTRUCTOR VỚI @Lazy
    public VNPayServiceImpl(
            TransactionRepository transactionRepository,
            @Lazy PaymentService paymentService) {
        this.transactionRepository = transactionRepository;
        this.paymentService = paymentService;
    }

    @Value("${vnpay.url}")
    private String vnpPayUrl;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    // ... rest of code giữ nguyên

    @Override
    public String createPaymentUrl(Long transactionId, BigDecimal amount, String orderInfo,
                                   String returnUrl, String bankCode) {
        log.info("Đang tạo URL thanh toán VNPay cho transaction: {}", transactionId);

        try {
            // Chuyển đổi số tiền sang đơn vị VNPay yêu cầu (VND * 100)
            long vnpAmount = amount.multiply(new BigDecimal("100")).longValue();

            // Tạo mã đơn hàng duy nhất
            String vnpTxnRef = String.valueOf(transactionId);

            // Lấy địa chỉ IP (có thể lấy từ request trong controller)
            String vnpIpAddr = "127.0.0.1";

            // Tạo thời gian tạo và hết hạn
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            String vnpCreateDate = formatter.format(cld.getTime());

            cld.add(Calendar.MINUTE, 15); // Hết hạn sau 15 phút
            String vnpExpireDate = formatter.format(cld.getTime());

            // Tạo map chứa các tham số
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnpVersion);
            vnpParams.put("vnp_Command", vnpCommand);
            vnpParams.put("vnp_TmnCode", vnpTmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnpReturnUrl);
            vnpParams.put("vnp_IpAddr", vnpIpAddr);
            vnpParams.put("vnp_CreateDate", vnpCreateDate);
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Thêm mã ngân hàng nếu có
            if (bankCode != null && !bankCode.isEmpty()) {
                vnpParams.put("vnp_BankCode", bankCode);
            }

            // Sắp xếp các tham số theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi hash data
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // Tạo secure hash
            String vnpSecureHash = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());
            String paymentUrl = vnpPayUrl + "?" + query.toString() + "&vnp_SecureHash=" + vnpSecureHash;

            log.info("Đã tạo URL thanh toán VNPay thành công cho transaction: {}", transactionId);
            return paymentUrl;

        } catch (UnsupportedEncodingException e) {
            log.error("Lỗi khi tạo URL thanh toán VNPay: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo URL thanh toán VNPay", e);
        }
    }

    @Override
    @Transactional
    public boolean handlePaymentCallback(Map<String, String> params) {
        log.info("Đang xử lý callback từ VNPay");

        // Xác thực chữ ký
        if (!verifyPaymentSignature(params)) {
            log.error("Chữ ký VNPay không hợp lệ");
            return false;
        }

        // Lấy thông tin từ params
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpBankCode = params.get("vnp_BankCode");
        String vnpCardType = params.get("vnp_CardType");

        log.info("VNPay callback - TxnRef: {}, ResponseCode: {}, TransactionNo: {}",
                vnpTxnRef, vnpResponseCode, vnpTransactionNo);

        // Tìm transaction
        Long transactionId = Long.parseLong(vnpTxnRef);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với ID: " + transactionId));

        // Lưu lại thông tin giao dịch VNPay cho cả success lẫn failure
        transaction.setVnpayTransactionNo(vnpTransactionNo);
        transaction.setVnpayBankCode(vnpBankCode);
        transaction.setVnpayCardType(vnpCardType);
        transactionRepository.save(transaction);

        // Kiểm tra mã phản hồi từ VNPay
        if ("00".equals(vnpResponseCode)) {
            // Thanh toán thành công
            log.info("Thanh toán VNPay thành công cho transaction: {}", transactionId);
            if (transaction.getSubscription() != null) {
                paymentService.completeSubscriptionPayment(transactionId, vnpTransactionNo, vnpBankCode, vnpCardType);
            } else {
                paymentService.completePayment(transactionId);
            }
            return true;
        } else {
            // Thanh toán thất bại
            String errorMessage = getVNPayErrorMessage(vnpResponseCode);
            log.error("Thanh toán VNPay thất bại cho transaction: {}, lỗi: {}",
                    transactionId, errorMessage);
            if (transaction.getSubscription() != null) {
                paymentService.handleFailedSubscriptionPayment(transactionId, errorMessage);
            } else {
                paymentService.handleFailedPayment(transactionId, errorMessage);
            }
            return false;
        }
    }

    @Override
    public boolean verifyPaymentSignature(Map<String, String> params) {
        log.info("Đang xác thực chữ ký VNPay");

        try {
            // Lấy chữ ký từ VNPay
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                log.error("Không tìm thấy chữ ký VNPay");
                return false;
            }

            // Loại bỏ các tham số không cần thiết
            Map<String, String> verifyParams = new HashMap<>(params);
            verifyParams.remove("vnp_SecureHash");
            verifyParams.remove("vnp_SecureHashType");

            // Sắp xếp các tham số
            List<String> fieldNames = new ArrayList<>(verifyParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi hash data
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = verifyParams.get(fieldName);

                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            // Tạo chữ ký mới để so sánh
            String signValue = VNPayConfig.hmacSHA512(vnpHashSecret, hashData.toString());

            boolean isValid = signValue.equals(vnpSecureHash);

            if (isValid) {
                log.info("Chữ ký VNPay hợp lệ");
            } else {
                log.error("Chữ ký VNPay không hợp lệ - Expected: {}, Actual: {}",
                        signValue, vnpSecureHash);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Lỗi khi xác thực chữ ký VNPay: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public String handleIPN(Map<String, String> params) {
        log.info("Đang xử lý IPN từ VNPay");

        // Xác thực chữ ký
        if (!verifyPaymentSignature(params)) {
            log.error("Chữ ký IPN không hợp lệ");
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Signature\"}";
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");

        try {
            Long transactionId = Long.parseLong(vnpTxnRef);
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElse(null);

            if (transaction == null) {
                log.error("Không tìm thấy giao dịch với ID: {}", transactionId);
                return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
            }

            // Kiểm tra trạng thái giao dịch
            if (transaction.getStatus() == Transaction.Status.SUCCESS) {
                log.info("Giao dịch đã được xử lý thành công trước đó");
                return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
            }

            // Xử lý theo mã phản hồi
            if ("00".equals(vnpResponseCode)) {
                paymentService.completePayment(transactionId);
                return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
            } else {
                String errorMessage = getVNPayErrorMessage(vnpResponseCode);
                paymentService.handleFailedPayment(transactionId, errorMessage);
                return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý IPN: {}", e.getMessage());
            return "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
        }
    }

    /**
     * Lấy thông báo lỗi từ mã phản hồi VNPay
     */
    private String getVNPayErrorMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65" -> "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch thất bại - Mã lỗi: " + responseCode;
        };
    }
}