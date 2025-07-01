package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.dto.PaymentRequest;
import edu.fpt.smokingcessionnew.dto.PaymentResponse;
import edu.fpt.smokingcessionnew.entity.MembershipPackage;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.MembershipPackageRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import edu.fpt.smokingcessionnew.service.PaymentService;
import edu.fpt.smokingcessionnew.util.VNPayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/packages")
    @ResponseBody
    @Operation(summary = "Lấy danh sách gói thành viên", description = "Lấy danh sách các gói thành viên hiện có")
    public List<MembershipPackage> getAllPackages() {
        return membershipPackageRepository.findByIsActiveTrue();
    }

    /**
     * Tạo giao dịch thanh toán chỉ với packageId
     * Tự động lấy userId từ token JWT và điền thông tin còn lại
     */
    @PostMapping("/create-payment-simple")
    @ResponseBody
    @Operation(
        summary = "Tạo giao dịch thanh toán đơn giản",
        description = "Tạo URL thanh toán VNPay với đầu vào đơn giản, chỉ cần packageId và bankCode (tùy chọn)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> createPaymentSimple(
            @RequestParam Integer packageId,
            @RequestParam(required = false, defaultValue = "NCB") String bankCode,
            HttpServletRequest request) {

        // Lấy thông tin người dùng từ token JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập");
        }

        String email = authentication.getName(); // Email của người dùng từ token
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        // Kiểm tra gói thành viên có tồn tại
        Optional<MembershipPackage> packageOptional = membershipPackageRepository.findById(packageId);
        if (packageOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Gói thành viên không tồn tại");
        }

        User user = userOptional.get();

        // Tạo đối tượng PaymentRequest đầy đủ
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setUserId(user.getId());
        paymentRequest.setPackageId(packageId);
        paymentRequest.setBankCode(bankCode);
        paymentRequest.setLanguage("vn"); // Mặc định tiếng Việt

        // Sử dụng VNPayUtil để lấy IP address đúng cách
        String ipAddress = VNPayUtil.getIpAddress(request);
        paymentRequest.setIpAddress(ipAddress);

        // Log IP address để debug
        logger.info("Client IP Address: {}", ipAddress);

        // Gọi service để tạo URL thanh toán
        ResponseEntity<?> response = paymentService.createPaymentUrl(paymentRequest);

        // Thêm debug: In URL trực tiếp để kiểm tra
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                Object body = response.getBody();
                if (body instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> respMap = (Map<String, Object>) body;
                    String paymentUrl = (String) respMap.get("paymentUrl");
                    // In URL để copy trực tiếp từ console
                    logger.info("=== URL THANH TOÁN (COPY TRỰC TIẾP) ===");
                    logger.info("{}", paymentUrl);
                    logger.info("=== KẾT THÚC URL ===");
                }
            } catch (Exception e) {
                logger.error("Không thể in URL thanh toán: {}", e.getMessage());
            }
        }

        return response;
    }

    @PostMapping("/create-payment")
    @ResponseBody
    @Operation(
        summary = "Tạo giao dịch thanh toán (đầy đủ tham số)",
        description = "Tạo URL thanh toán VNPay cho gói thành viên đã chọn",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> createPayment(
            @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {

        // Set IP address cho yêu cầu thanh toán sử dụng VNPayUtil
        String ipAddress = VNPayUtil.getIpAddress(request);
        paymentRequest.setIpAddress(ipAddress);

        // Log IP address để debug
        logger.info("Client IP Address from VNPayUtil: {}", ipAddress);

        return paymentService.createPaymentUrl(paymentRequest);
    }

    /**
     * Xử lý kết quả thanh toán từ VNPay và hiển thị trang kết quả
     * Phương thức này trả về template Thymeleaf thay vì ResponseEntity
     */
    @GetMapping("/vnpay-return")
    @Operation(summary = "Xử lý kết quả thanh toán", description = "Endpoint xử lý kết quả trả về từ cổng thanh toán VNPay")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> vnpParams = new HashMap<>();

        // Lấy tất cả tham số trả về từ VNPay
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            vnpParams.put(paramName, paramValue);
        }

        // Log thông tin để gỡ lỗi
        logger.info("VNPay return parameters: {}", vnpParams);

        // Gọi service để xử lý kết quả thanh toán
        ResponseEntity<?> responseEntity = paymentService.processPaymentReturn(vnpParams);

        // Trích xuất thông tin để hiển thị trên template
        Object responseBody = responseEntity.getBody();
        if (responseBody instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) responseBody;

            // Thêm dữ liệu vào Model để hiển thị trong view
            model.addAllAttributes(responseMap);

            // Chuyển đổi số tiền từ string sang số (nếu có)
            if (responseMap.containsKey("amount")) {
                try {
                    String amountStr = responseMap.get("amount").toString();
                    model.addAttribute("amount", Long.parseLong(amountStr));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse amount to long: {}", responseMap.get("amount"));
                }
            }

            return "payment-result"; // Trả về template payment-result.html
        } else {
            // Xử lý trường hợp không nhận được response dạng Map
            model.addAttribute("status", "failed");
            model.addAttribute("message", "Đã xảy ra lỗi không xác định trong quá trình thanh toán");
            model.addAttribute("supportPhone", "1900 55 55 77");
            return "payment-result";
        }
    }

    /**
     * API endpoint xử lý IPN từ VNPay (gọi bởi VNPay server)
     * API này giữ nguyên vì được gọi từ VNPay, không phải từ người dùng
     */
    @PostMapping("/vnpay-ipn")
    @ResponseBody
    @Operation(summary = "IPN (Instant Payment Notification)", description = "Endpoint để VNPay gửi thông báo tự động về trạng thái thanh toán")
    public ResponseEntity<?> vnpayIpn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();

        // Lấy tất cả tham số từ request
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            vnpParams.put(paramName, paramValue);
        }

        // Xử lý IPN từ VNPay (có thể sử dụng lại processPaymentReturn)
        return paymentService.processPaymentReturn(vnpParams);
    }

    /**
     * API để truy vấn trạng thái giao dịch (dùng cho kiểm tra phía client)
     */
    @GetMapping("/transaction-status")
    @ResponseBody
    @Operation(summary = "Truy vấn trạng thái giao dịch", description = "Kiểm tra trạng thái của một giao dịch thanh toán")
    public ResponseEntity<?> getTransactionStatus(@RequestParam String transactionId) {
        // Implement logic to check transaction status
        // This is just a placeholder - you would typically query your database here
        return ResponseEntity.ok().body(Map.of(
            "transactionId", transactionId,
            "status", "PENDING",
            "message", "Trạng thái giao dịch đang được xử lý"
        ));
    }
}
