package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.config.VNPayConfig;
import edu.fpt.smokingcessionnew.dto.PaymentRequest;
import edu.fpt.smokingcessionnew.dto.PaymentResponse;
import edu.fpt.smokingcessionnew.entity.MemberSubscription;
import edu.fpt.smokingcessionnew.entity.MembershipPackage;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.MemberSubscriptionRepository;
import edu.fpt.smokingcessionnew.repository.MembershipPackageRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    // VNPay response codes and explanations
    private static final Map<String, String> VNP_RESPONSE_CODES = new HashMap<>();

    static {
        VNP_RESPONSE_CODES.put("00", "Giao dịch thành công");
        VNP_RESPONSE_CODES.put("01", "Giao dịch đã tồn tại");
        VNP_RESPONSE_CODES.put("02", "Merchant không hợp lệ");
        VNP_RESPONSE_CODES.put("03", "Dữ liệu gửi sang không đúng định dạng");
        VNP_RESPONSE_CODES.put("04", "Khởi tạo giao dịch không thành công do Website đang bị tạm khóa");
        VNP_RESPONSE_CODES.put("05", "Giao dịch không thành công do: Quý khách nhập sai mật khẩu quá số lần quy định");
        VNP_RESPONSE_CODES.put("06", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch");
        VNP_RESPONSE_CODES.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)");
        VNP_RESPONSE_CODES.put("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking");
        VNP_RESPONSE_CODES.put("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        VNP_RESPONSE_CODES.put("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán");
        VNP_RESPONSE_CODES.put("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa");
        VNP_RESPONSE_CODES.put("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch");
        VNP_RESPONSE_CODES.put("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch");
        VNP_RESPONSE_CODES.put("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch");
        VNP_RESPONSE_CODES.put("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày");
        VNP_RESPONSE_CODES.put("75", "Ngân hàng thanh toán đang bảo trì");
        VNP_RESPONSE_CODES.put("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định");
        VNP_RESPONSE_CODES.put("99", "Các lỗi khác");
    }

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberSubscriptionRepository memberSubscriptionRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Tạo URL thanh toán VNPay
     *
     * @param paymentRequest thông tin thanh toán
     * @return URL thanh toán VNPay
     */
    public ResponseEntity<?> createPaymentUrl(PaymentRequest paymentRequest) {
        try {
            // Kiểm tra user và package
            Optional<User> userOptional = userRepository.findById(paymentRequest.getUserId());
            if (userOptional.isEmpty()) {
                logger.error("Payment failed: User with ID {} not found", paymentRequest.getUserId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Người dùng không tồn tại");
            }

            Optional<MembershipPackage> packageOptional = membershipPackageRepository.findById(paymentRequest.getPackageId());
            if (packageOptional.isEmpty()) {
                logger.error("Payment failed: Package with ID {} not found", paymentRequest.getPackageId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gói thành viên không tồn tại");
            }

            MembershipPackage membershipPackage = packageOptional.get();
            User user = userOptional.get();

            // Tạo một mã giao dịch duy nhất
            String vnpTxnRef = getTransactionNumber();

            // Chuẩn hóa OrderInfo - chỉ sử dụng ASCII không dấu
            String vnpOrderInfo = "Thanh toan goi " + removeAccent(membershipPackage.getPackageName()) + " cho tai khoan " + user.getEmail();

            // Số tiền phải nhân với 100 (VND không có phần thập phân)
            String vnpAmount = String.valueOf(membershipPackage.getPrice().multiply(new java.math.BigDecimal("100")).longValue());

            logger.info("Creating payment for amount: {} VND", membershipPackage.getPrice());
            logger.info("Order Info: {}", vnpOrderInfo);

            // Lưu thông tin đăng ký gói thành viên
            MemberSubscription subscription = new MemberSubscription();
            subscription.setUser(user);
            subscription.setMembershipPackage(membershipPackage);
            subscription.setCreatedDate(LocalDateTime.now());
            subscription.setAmountPaid(membershipPackage.getPrice());
            subscription.setStatus(0); // 0: Pending
            subscription.setPaymentStatus(0); // 0: Pending
            subscription.setTransactionId(vnpTxnRef);
            memberSubscriptionRepository.save(subscription);

            logger.info("Created subscription record with transaction ID: {}", vnpTxnRef);

            // Tạo tham số URL thanh toán
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnPayConfig.getVnpVersion());
            vnpParams.put("vnp_Command", vnPayConfig.getVnpCommand());
            vnpParams.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
            vnpParams.put("vnp_Amount", vnpAmount);
            vnpParams.put("vnp_CurrCode", vnPayConfig.getVnpCurrCode());

            // Chỉ thêm bankCode nếu có giá trị
            if (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().isEmpty()) {
                vnpParams.put("vnp_BankCode", paymentRequest.getBankCode());
            }

            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
            vnpParams.put("vnp_OrderType", "190000"); // Mã danh mục hàng hóa: 190000 - thanh toán dịch vụ

            // Đảm bảo locale đúng định dạng VNPay yêu cầu (vn hoặc en)
            String locale = "vn";
            if (paymentRequest.getLanguage() != null &&
                (paymentRequest.getLanguage().equals("vn") || paymentRequest.getLanguage().equals("en"))) {
                locale = paymentRequest.getLanguage();
            }
            vnpParams.put("vnp_Locale", locale);

            vnpParams.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());

            // Đảm bảo IP không null và đúng định dạng
            String ipAddr = "127.0.0.1"; // IP mặc định nếu không có
            if (paymentRequest.getIpAddress() != null && !paymentRequest.getIpAddress().isEmpty()) {
                ipAddr = paymentRequest.getIpAddress();
            }
            vnpParams.put("vnp_IpAddr", ipAddr);

            // Tạo ngày thanh toán với múi giờ GMT+7 (Việt Nam)
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            // Tạo ngày hết hạn (15 phút sau khi tạo)
            cld.add(Calendar.MINUTE, 15);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // In thông tin các tham số đã tạo để debug
            logger.info("VNPay parameters: {}", vnpParams);

            // Sắp xếp tham số theo thứ tự từ điển (yêu cầu bắt buộc của VNPay)
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi query và hash
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    // QUAN TRỌNG: VNPay yêu cầu chuỗi hash KHÔNG được URL encode
                    hashData.append(fieldName).append('=').append(fieldValue);

                    // Còn chuỗi query cần URL encode cho cả key và value
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // Log dữ liệu hash để debug
            logger.info("Hash data before encryption: {}", hashData.toString());

            // Tạo chữ ký
            String vnpSecureHash = hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData.toString());
            logger.info("Secure hash: {}", vnpSecureHash);
            logger.info("Hash secret used: {}", vnPayConfig.getVnpHashSecret());

            // Thêm SecureHash vào URL đã được encode
            query.append("&").append(URLEncoder.encode("vnp_SecureHash", StandardCharsets.UTF_8))
                 .append("=").append(vnpSecureHash);

            // Tạo URL thanh toán đầy đủ
            String paymentUrl = vnPayConfig.getVnpPayUrl() + "?" + query;

            // Log URL thanh toán
            logger.info("VNPay payment URL: {}", vnPayConfig.getVnpPayUrl());
            logger.info("Full URL length: {}", paymentUrl.length());
            // Hiển thị URL đầy đủ để debug nếu cần
            logger.debug("Full payment URL: {}", paymentUrl);

            // Kiểm tra URL có hợp lệ không
            if (paymentUrl.length() > 2000) {
                logger.warn("Payment URL length exceeds 2000 characters, may cause issues in some browsers");
            }

            // Trả về URL thanh toán
            Map<String, Object> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("transactionId", vnpTxnRef);
            return ResponseEntity.ok(response);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Payment URL generation failed: Error creating signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi tạo chữ ký: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Payment URL generation failed: Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi không xác định: " + e.getMessage());
        }
    }

    /**
     * Xử lý kết quả trả về từ VNPay
     *
     * @param vnpParams tham số trả về từ VNPay
     * @return Kết quả xác nhận thanh toán
     */
    public ResponseEntity<?> processPaymentReturn(Map<String, String> vnpParams) {
        try {
            String vnpSecureHash = vnpParams.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                logger.error("Payment verification failed: Missing secure hash");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không tìm thấy chữ ký bảo mật");
            }

            // Extract important parameters first before any modifications
            final String vnpTxnRef = vnpParams.get("vnp_TxnRef");
            final String vnpResponseCode = vnpParams.get("vnp_ResponseCode");

            // Log transaction details for debugging
            logger.info("Processing payment return - TxnRef: {}, ResponseCode: {}", vnpTxnRef, vnpResponseCode);

            // Xóa params không cần thiết
            String signValue = vnpSecureHash;
            Map<String, String> paramsCopy = new HashMap<>(vnpParams);
            paramsCopy.remove("vnp_SecureHashType");
            paramsCopy.remove("vnp_SecureHash");

            // Kiểm tra chữ ký
            String checkSignature = hashAllFields(paramsCopy);

            if (signValue.equals(checkSignature)) {
                Optional<MemberSubscription> subscriptionOptional = memberSubscriptionRepository.findByTransactionId(vnpTxnRef);
                if (subscriptionOptional.isEmpty()) {
                    logger.error("Payment verification failed: No subscription found for transaction ID {}", vnpTxnRef);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(createErrorResponse("Không tìm thấy giao dịch", vnpResponseCode));
                }

                MemberSubscription subscription = subscriptionOptional.get();

                // Nếu thanh toán thành công (00)
                if ("00".equals(vnpResponseCode)) {
                    logger.info("Payment succeeded for transaction {}", vnpTxnRef);

                    // Cập nhật trạng thái đăng ký
                    subscription.setPaymentStatus(1); // 1: Completed
                    subscription.setStatus(1); // 1: Active
                    subscription.setPaymentMethod("VNPay");
                    subscription.setPaymentDate(LocalDate.now());

                    // Tính toán ngày hết hạn
                    LocalDate startDate = LocalDate.now();
                    LocalDate endDate = startDate.plusDays(subscription.getMembershipPackage().getDurationDays());

                    subscription.setStartDate(startDate);
                    subscription.setEndDate(endDate);
                    subscription.setUpdatedDate(LocalDateTime.now());

                    memberSubscriptionRepository.save(subscription);

                    // Tạo phản hồi thành công
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Thanh toán thành công");
                    response.put("transactionId", vnpTxnRef);
                    response.put("amount", vnpParams.get("vnp_Amount"));
                    response.put("orderInfo", vnpParams.get("vnp_OrderInfo"));

                    // Gửi email xác nhận thanh toán
                    try {
                        // Implement email sending logic if needed
                    } catch (Exception e) {
                        logger.warn("Could not send confirmation email: {}", e.getMessage());
                    }

                    return ResponseEntity.ok(response);
                } else {
                    // Nếu thanh toán thất bại
                    logger.warn("Payment failed for transaction {} with response code: {}", vnpTxnRef, vnpResponseCode);
                    subscription.setPaymentStatus(2); // 2: Failed
                    subscription.setUpdatedDate(LocalDateTime.now());
                    memberSubscriptionRepository.save(subscription);

                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(createErrorResponse("Thanh toán thất bại", vnpResponseCode));
                }
            } else {
                logger.error("Payment verification failed: Invalid signature for transaction {}", vnpTxnRef);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Chữ ký không hợp lệ", null));
            }
        } catch (Exception e) {
            logger.error("Error processing payment return", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi xử lý kết quả thanh toán: " + e.getMessage(), null));
        }
    }

    /**
     * Tạo đối tượng phản hồi lỗi chi tiết
     */
    private Map<String, Object> createErrorResponse(String message, String responseCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failed");
        response.put("message", message);

        if (responseCode != null) {
            response.put("responseCode", responseCode);
            response.put("responseMessage", VNP_RESPONSE_CODES.getOrDefault(responseCode, "Lỗi không xác định"));
            response.put("contactSupport", true);
            response.put("supportPhone", "1900 55 55 77"); // Replace with your actual support contact
        }

        return response;
    }

    /**
     * Tạo mã giao dịch ngẫu nhiên
     *
     * @return Mã giao dịch
     */
    private String getTransactionNumber() {
        return String.valueOf(System.currentTimeMillis()) + generateRandomNumber(8);
    }

    /**
     * Tạo số ngẫu nhiên với độ dài xác định
     *
     * @param length độ dài của số
     * @return Chuỗi số ngẫu nhiên
     */
    private String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Tạo chữ ký HMAC_SHA512
     *
     * @param key  khóa bí mật
     * @param data dữ liệu cần tạo chữ ký
     * @return Chuỗi chữ ký
     */
    private String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKeySpec);
        byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(result);
    }

    /**
     * Chuyển đổi mảng byte sang chuỗi hex
     *
     * @param bytes mảng byte
     * @return Chuỗi hex
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Tạo chữ ký để xác thực kết quả trả về từ VNPay
     *
     * @param fields tham số kết quả
     * @return Chữ ký
     */
    private String hashAllFields(Map<String, String> fields) throws NoSuchAlgorithmException, InvalidKeyException {
        // Sắp xếp tham số theo thứ tự từ điển
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        // Tạo chuỗi hash
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Không URL encode các giá trị khi tạo chữ ký để xác thực
                sb.append(fieldName).append("=").append(fieldValue);
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }

        logger.info("Hash verification data: {}", sb.toString());
        return hmacSHA512(vnPayConfig.getVnpHashSecret(), sb.toString());
    }

    /**
     * Loại bỏ dấu tiếng Việt và ký tự đặc biệt
     *
     * @param input chuỗi đầu vào
     * @return chuỗi đã được chuẩn hóa
     */
    private String removeAccent(String input) {
        if (input == null) return null;

        // Chuyển đổi sang dạng không dấu
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}

