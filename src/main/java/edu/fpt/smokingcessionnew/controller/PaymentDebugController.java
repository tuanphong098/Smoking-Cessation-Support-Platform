package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.dto.PaymentRequest;
import edu.fpt.smokingcessionnew.entity.MembershipPackage;
import edu.fpt.smokingcessionnew.repository.MembershipPackageRepository;
import edu.fpt.smokingcessionnew.service.PaymentService;
import edu.fpt.smokingcessionnew.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller để debug các vấn đề liên quan đến thanh toán VNPay
 * Chỉ nên được sử dụng trong môi trường phát triển
 */
@RestController
@RequestMapping("/api/payment-debug")
public class PaymentDebugController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDebugController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    /**
     * Trả về URL thanh toán dạng HTML để có thể nhấp vào trực tiếp
     */
    @GetMapping("/test-payment-url")
    public String testPaymentUrl(
            @RequestParam Integer packageId,
            @RequestParam Integer userId,
            @RequestParam(required = false, defaultValue = "NCB") String bankCode,
            HttpServletRequest request) {

        // Tạo PaymentRequest
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setUserId(userId);
        paymentRequest.setPackageId(packageId);
        paymentRequest.setBankCode(bankCode);
        paymentRequest.setLanguage("vn");
        paymentRequest.setIpAddress(request.getRemoteAddr());

        try {
            ResponseEntity<?> response = paymentService.createPaymentUrl(paymentRequest);

            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                String paymentUrl = (String) responseBody.get("paymentUrl");
                String transactionId = (String) responseBody.get("transactionId");

                StringBuilder html = new StringBuilder();
                html.append("<html><head><title>VNPay Test Payment URL</title>");
                html.append("<style>body{font-family:Arial,sans-serif;margin:20px;line-height:1.6;}");
                html.append(".container{max-width:800px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:5px;}");
                html.append("h1{color:#2c3e50;}h2{color:#3498db;}");
                html.append(".url-box{background:#f8f9fa;padding:15px;border-radius:5px;word-break:break-all;margin:20px 0;}");
                html.append(".button{display:inline-block;background:#3498db;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;margin-top:10px;}");
                html.append(".button:hover{background:#2980b9;}</style></head><body><div class='container'>");

                html.append("<h1>VNPay Test Payment URL</h1>");

                // Hiển thị thông tin gói thành viên
                Optional<MembershipPackage> packageOpt = membershipPackageRepository.findById(packageId);
                if (packageOpt.isPresent()) {
                    MembershipPackage pkg = packageOpt.get();
                    html.append("<h2>Thông tin gói</h2>");
                    html.append("<p><strong>Tên gói:</strong> ").append(pkg.getPackageName()).append("</p>");
                    html.append("<p><strong>Giá:</strong> ").append(pkg.getPrice()).append(" VNĐ</p>");
                    html.append("<p><strong>Thời hạn:</strong> ").append(pkg.getDurationDays()).append(" ngày</p>");
                }

                // Hiển thị thông tin thanh toán
                html.append("<h2>Thông tin thanh toán</h2>");
                html.append("<p><strong>Mã giao dịch:</strong> ").append(transactionId).append("</p>");
                html.append("<p><strong>Ngân hàng:</strong> ").append(bankCode).append("</p>");

                // Hiển thị URL thanh toán
                html.append("<h2>URL Thanh toán</h2>");
                html.append("<div class='url-box'>").append(paymentUrl).append("</div>");

                // Tạo link trực tiếp
                html.append("<p><a href='").append(paymentUrl).append("' target='_blank' class='button'>Mở trang thanh toán</a></p>");

                // Thông tin test
                html.append("<h2>Thông tin test VNPay Sandbox</h2>");
                html.append("<p>Sử dụng thông tin thẻ test sau:</p>");
                html.append("<ul>");
                html.append("<li><strong>Số thẻ:</strong> 9704198526191432198</li>");
                html.append("<li><strong>Họ tên:</strong> NGUYEN VAN A</li>");
                html.append("<li><strong>Ngày hết hạn:</strong> 07/15</li>");
                html.append("<li><strong>Mã OTP:</strong> 123456</li>");
                html.append("</ul>");

                html.append("</div></body></html>");

                return html.toString();
            } else {
                return "Lỗi: " + response.getBody().toString();
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tạo URL thanh toán: ", e);
            return "Đã xảy ra lỗi: " + e.getMessage();
        }
    }

    /**
     * Hiển thị danh sách các gói thành viên để test
     */
    @GetMapping("/packages")
    public String listPackages() {
        List<MembershipPackage> packages = membershipPackageRepository.findByIsActiveTrue();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Danh sách gói thành viên</title>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:20px;line-height:1.6;}");
        html.append(".container{max-width:800px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:5px;}");
        html.append("table{width:100%;border-collapse:collapse;margin:20px 0;}");
        html.append("th,td{padding:10px;text-align:left;border-bottom:1px solid #ddd;}");
        html.append("th{background:#f2f2f2;}");
        html.append("tr:hover{background:#f5f5f5;}");
        html.append(".button{display:inline-block;background:#3498db;color:white;padding:5px 10px;text-decoration:none;border-radius:3px;}");
        html.append(".button:hover{background:#2980b9;}</style></head><body><div class='container'>");

        html.append("<h1>Danh sách gói thành viên</h1>");

        html.append("<p>Nhập ID người dùng để test:</p>");
        html.append("<input type='text' id='userId' placeholder='Nhập user ID' style='padding:5px;width:200px;'>");

        html.append("<table>");
        html.append("<tr><th>ID</th><th>Tên gói</th><th>Giá (VND)</th><th>Thời hạn (ngày)</th><th>Thao tác</th></tr>");

        for (MembershipPackage pkg : packages) {
            html.append("<tr>");
            html.append("<td>").append(pkg.getId()).append("</td>");
            html.append("<td>").append(pkg.getPackageName()).append("</td>");
            html.append("<td>").append(pkg.getPrice()).append("</td>");
            html.append("<td>").append(pkg.getDurationDays()).append("</td>");
            html.append("<td><a href='#' onclick='testPayment(").append(pkg.getId()).append(")' class='button'>Test thanh toán</a></td>");
            html.append("</tr>");
        }

        html.append("</table>");

        // JavaScript để chuyển hướng đến trang test thanh toán
        html.append("<script>");
        html.append("function testPayment(packageId) {");
        html.append("  var userId = document.getElementById('userId').value;");
        html.append("  if (!userId || isNaN(userId)) {");
        html.append("    alert('Vui lòng nhập ID người dùng hợp lệ!');");
        html.append("    return;");
        html.append("  }");
        html.append("  window.location.href = '/api/payment-debug/test-payment-url?packageId=' + packageId + '&userId=' + userId;");
        html.append("}");
        html.append("</script>");

        html.append("</div></body></html>");

        return html.toString();
    }
}
