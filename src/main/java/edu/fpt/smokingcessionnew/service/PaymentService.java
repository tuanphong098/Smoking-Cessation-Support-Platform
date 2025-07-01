package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.config.VNPayConfig;
import edu.fpt.smokingcessionnew.dto.PaymentRequest;
import edu.fpt.smokingcessionnew.entity.MemberSubscription;
import edu.fpt.smokingcessionnew.entity.MembershipPackage;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.MemberSubscriptionRepository;
import edu.fpt.smokingcessionnew.repository.MembershipPackageRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import edu.fpt.smokingcessionnew.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberSubscriptionRepository memberSubscriptionRepository;

    /**
     * Tạo URL thanh toán VNPay theo chuẩn từ phamanhduc.com
     */
    public ResponseEntity<?> createPaymentUrl(PaymentRequest paymentRequest) {
        try {
            // Validate request
            if (paymentRequest.getUserId() == null || paymentRequest.getPackageId() == null) {
                return ResponseEntity.badRequest().body("User ID và Package ID không được null");
            }

            // Lấy thông tin user và package
            Optional<User> userOpt = userRepository.findById(paymentRequest.getUserId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Người dùng không tồn tại");
            }

            Optional<MembershipPackage> packageOpt = membershipPackageRepository.findById(paymentRequest.getPackageId());
            if (packageOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Gói thành viên không tồn tại");
            }

            User user = userOpt.get();
            MembershipPackage membershipPackage = packageOpt.get();

            // Tạo transaction reference
            String vnp_TxnRef = VNPayUtil.getCurrentTimeString() + VNPayUtil.getRandomNumber(6);

            // Lưu subscription record
            MemberSubscription subscription = new MemberSubscription();
            subscription.setUser(user);
            subscription.setMembershipPackage(membershipPackage);
            subscription.setTransactionId(vnp_TxnRef);
            subscription.setAmountPaid(membershipPackage.getPrice());
            subscription.setStatus(0); // Pending
            subscription.setPaymentStatus(0); // Pending
            subscription.setCreatedDate(LocalDateTime.now());
            memberSubscriptionRepository.save(subscription);

            // Tạo VNPay parameters - ĐẢMN BẢO ĐẦY ĐỦ TẤT CẢ THAM SỐ BẮT BUỘC
            Map<String, String> vnp_Params = new LinkedHashMap<>();

            // Các tham số bắt buộc theo tài liệu VNPay
            vnp_Params.put("vnp_Version", VNPayConfig.VERSION);
            vnp_Params.put("vnp_Command", VNPayConfig.COMMAND);
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());

            // Số tiền * 100 (VNPay yêu cầu đơn vị: xu)
            long amount = membershipPackage.getPrice().multiply(BigDecimal.valueOf(100)).longValue();
            vnp_Params.put("vnp_Amount", String.valueOf(amount));

            vnp_Params.put("vnp_CurrCode", VNPayConfig.CURRENCY_CODE);

            // Bank code (tùy chọn nhưng nên có)
            if (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().trim().isEmpty()) {
                vnp_Params.put("vnp_BankCode", paymentRequest.getBankCode());
            }

            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

            // OrderInfo - đã fix encoding issue
            String orderInfo = "Thanh toan goi " + membershipPackage.getPackageName();
            vnp_Params.put("vnp_OrderInfo", orderInfo);

            vnp_Params.put("vnp_OrderType", VNPayConfig.ORDER_TYPE);
            vnp_Params.put("vnp_Locale", VNPayConfig.LOCALE);
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());

            // IP Address - sử dụng hàm mới để đảm bảo IPv4
            String clientIp = paymentRequest.getIpAddress();
            if (clientIp == null || clientIp.trim().isEmpty()) {
                clientIp = "127.0.0.1";
            }
            // Validate và chuyển về IPv4 nếu cần
            if (!isValidIPv4(clientIp)) {
                clientIp = "127.0.0.1";
            }
            vnp_Params.put("vnp_IpAddr", clientIp);

            vnp_Params.put("vnp_CreateDate", VNPayUtil.getCurrentTimeString());
            vnp_Params.put("vnp_ExpireDate", VNPayUtil.getExpireTimeString());

            // Thêm các tham số tùy chọn khác (nếu có)
            if (paymentRequest.getLanguage() != null && !paymentRequest.getLanguage().trim().isEmpty()) {
                vnp_Params.put("vnp_Locale", paymentRequest.getLanguage());
            }

            // Log để debug - in ra tất cả parameters trước khi tạo signature
            logger.info("VNPay parameters before signing:");
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                logger.info("{}: {}", entry.getKey(), entry.getValue());
            }

            // Tạo URL thanh toán với signature đúng
            String queryUrl = VNPayUtil.createPaymentUrl(vnp_Params, vnPayConfig.getVnpHashSecret());
            String paymentUrl = vnPayConfig.getVnpPaymentUrl() + "?" + queryUrl;

            logger.info("Created VNPay payment URL for transaction: {}", vnp_TxnRef);
            logger.info("Payment URL length: {}", paymentUrl.length());

            // Trả về response
            Map<String, Object> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("transactionId", vnp_TxnRef);
            response.put("amount", membershipPackage.getPrice());
            response.put("packageName", membershipPackage.getPackageName());
            response.put("vnpTmnCode", vnPayConfig.getVnpTmnCode()); // Để debug
            response.put("clientIp", clientIp); // Để debug

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating payment URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi tạo URL thanh toán: " + e.getMessage());
        }
    }

    /**
     * Helper method để validate IPv4
     */
    private boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Xử lý kết quả thanh toán từ VNPay
     */
    public ResponseEntity<?> processPaymentReturn(Map<String, String> vnpParams) {
        try {
            logger.info("Processing VNPay return with parameters: {}", vnpParams.keySet());

            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");

            if (vnp_TxnRef == null || vnp_ResponseCode == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin giao dịch");
            }

            // Validate signature
            Map<String, String> paramsToValidate = new HashMap<>(vnpParams);
            boolean isValidSignature = VNPayUtil.validateSignature(paramsToValidate, vnPayConfig.getVnpHashSecret());

            if (!isValidSignature) {
                logger.error("Invalid signature for transaction: {}", vnp_TxnRef);
                return ResponseEntity.badRequest().body("Chữ ký không hợp lệ");
            }

            // Tìm subscription
            Optional<MemberSubscription> subscriptionOpt = memberSubscriptionRepository.findByTransactionId(vnp_TxnRef);
            if (subscriptionOpt.isEmpty()) {
                logger.error("No subscription found for transaction: {}", vnp_TxnRef);
                return ResponseEntity.badRequest().body("Không tìm thấy giao dịch");
            }

            MemberSubscription subscription = subscriptionOpt.get();

            // Xử lý theo response code
            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công
                subscription.setPaymentStatus(1); // Completed
                subscription.setStatus(1); // Active
                subscription.setPaymentMethod("VNPay");
                subscription.setPaymentDate(LocalDate.now());

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(subscription.getMembershipPackage().getDurationDays());
                subscription.setStartDate(startDate);
                subscription.setEndDate(endDate);
                subscription.setUpdatedDate(LocalDateTime.now());

                memberSubscriptionRepository.save(subscription);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Thanh toán thành công");
                response.put("transactionId", vnp_TxnRef);
                response.put("amount", vnpParams.get("vnp_Amount"));
                response.put("packageName", subscription.getMembershipPackage().getPackageName());

                return ResponseEntity.ok(response);
            } else {
                // Thanh toán thất bại
                subscription.setPaymentStatus(2); // Failed
                subscription.setUpdatedDate(LocalDateTime.now());
                memberSubscriptionRepository.save(subscription);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "failed");
                response.put("message", "Thanh toán thất bại");
                response.put("responseCode", vnp_ResponseCode);
                response.put("transactionId", vnp_TxnRef);

                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("Error processing VNPay return", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý kết quả thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách packages
     */
    public List<MembershipPackage> getAllActivePackages() {
        return membershipPackageRepository.findByIsActiveTrue();
    }

    /**
     * Kiểm tra trạng thái giao dịch
     */
    public ResponseEntity<?> getTransactionStatus(String transactionId) {
        Optional<MemberSubscription> subscriptionOpt = memberSubscriptionRepository.findByTransactionId(transactionId);

        if (subscriptionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MemberSubscription subscription = subscriptionOpt.get();
        Map<String, Object> status = new HashMap<>();
        status.put("transactionId", transactionId);
        status.put("paymentStatus", subscription.getPaymentStatus());
        status.put("status", subscription.getStatus());
        status.put("amount", subscription.getAmountPaid());
        status.put("packageName", subscription.getMembershipPackage().getPackageName());
        status.put("createdDate", subscription.getCreatedDate());

        if (subscription.getPaymentDate() != null) {
            status.put("paymentDate", subscription.getPaymentDate());
        }

        return ResponseEntity.ok(status);
    }
}
