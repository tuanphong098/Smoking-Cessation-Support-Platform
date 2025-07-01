package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import edu.fpt.smokingcessionnew.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller chỉ dành cho môi trường phát triển
 * Giúp lấy link xác thực email mà không cần gửi email thật
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    /**
     * Lấy link xác thực cho email đã đăng ký
     * CHỈ DÀNH CHO MÔI TRƯỜNG PHÁT TRIỂN
     */
    @GetMapping("/verification-link")
    public ResponseEntity<?> getVerificationLink(@RequestParam String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy người dùng với email: " + email);
        }

        User user = userOptional.get();

        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Email này đã được xác thực rồi");
        }

        if (user.getVerificationToken() == null) {
            return ResponseEntity.badRequest().body("Không có token xác thực cho email này");
        }

        String verificationUrl = appUrl + "/api/auth/verify-email?token=" + user.getVerificationToken();

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("verificationUrl", verificationUrl);
        response.put("token", user.getVerificationToken());
        response.put("expiryDate", user.getTokenExpiryDate());
        response.put("message", "Click vào link bên dưới để xác thực email");

        return ResponseEntity.ok(response);
    }

    /**
     * Hiển thị tất cả người dùng chưa xác thực email
     */
    @GetMapping("/unverified-users")
    public ResponseEntity<?> getUnverifiedUsers() {
        // Tìm users chưa xác thực email
        var users = userRepository.findAll().stream()
            .filter(user -> user.getEmailVerified() == null || !user.getEmailVerified())
            .map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("email", user.getEmail());
                userInfo.put("fullName", user.getFullName());
                userInfo.put("hasToken", user.getVerificationToken() != null);
                userInfo.put("tokenExpiry", user.getTokenExpiryDate());
                return userInfo;
            })
            .toList();

        return ResponseEntity.ok(users);
    }

    /**
     * Test gửi email thật - CHỈ DÀNH CHO PHÁT TRIỂN
     */
    @PostMapping("/test-send-email")
    public ResponseEntity<?> testSendEmail(@RequestParam String email) {
        try {
            // Tìm user theo email
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng với email: " + email);
            }

            User user = userOptional.get();

            // Tạo token mới nếu chưa có
            if (user.getVerificationToken() == null) {
                String newToken = java.util.UUID.randomUUID().toString();
                user.setVerificationToken(newToken);
                user.setTokenExpiryDate(java.time.LocalDateTime.now().plusHours(24));
                userRepository.save(user);
            }

            // Gọi EmailService để gửi email thật
            emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken(), user.getFullName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email đã được gửi thành công đến: " + email);
            response.put("note", "Kiểm tra hộp thư đến và spam của bạn");
            response.put("verificationToken", user.getVerificationToken());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gửi email thất bại: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
