package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.dto.AuthResponse;
import edu.fpt.smokingcessionnew.dto.LoginRequest;
import edu.fpt.smokingcessionnew.dto.RegisterRequest;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import edu.fpt.smokingcessionnew.security.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    public ResponseEntity<?> register(RegisterRequest request) {
        // Kiểm tra password và confirmPassword
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại trong hệ thống");
        }

        // Kiểm tra vai trò hợp lệ (2 = Member, 3 = Coach)
        if (request.getRole() != 2 && request.getRole() != 3) {
            return ResponseEntity.badRequest().body("Vai trò không hợp lệ. Chỉ chấp nhận vai trò là Member (2) hoặc Coach (3)");
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole()); // Sử dụng vai trò từ request (2=Member hoặc 3=Coach)
        user.setStatus(0); // 0 = Inactive (chưa xác thực)
        user.setCreatedDate(LocalDateTime.now());

        // Bỏ qua các trường không tồn tại trong DB
        // user.setEmailVerified(false);
        // user.setVerificationToken(verificationToken);
        // user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        // Tạo token xác thực email nhưng lưu trong bộ nhớ tạm hoặc redis/cache thay vì DB
        String verificationToken = UUID.randomUUID().toString();

        // Lưu token vào một nơi khác, hoặc sử dụng thêm bảng riêng cho xác thực
        // Ví dụ có thể lưu vào cache/Redis với key là email của user

        User savedUser = userRepository.save(user);

        try {
            // Gửi email xác thực
            emailService.sendVerificationEmail(user.getEmail(), verificationToken, user.getFullName());
            return ResponseEntity.ok("Đăng ký thành công! Email xác nhận đã được gửi đến hộp thư của bạn.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đăng ký thành công nhưng không thể gửi email xác nhận: " + e.getMessage());
        }
    }

    public ResponseEntity<?> login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email hoặc mật khẩu không đúng");
        }

        User user = userOpt.get();

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Email hoặc mật khẩu không đúng");
        }

        // Kiểm tra trạng thái kích hoạt tài khoản
        if (user.getStatus() != 1) { // 1 = Active
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tài khoản chưa được kích hoạt hoặc đã bị khóa. Vui lòng kiểm tra email để xác thực.");
        }

        // Kiểm tra vai trò người dùng
        if (!user.getRole().equals(request.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Vai trò không đúng với tài khoản này.");
        }

        // Cập nhật lần đăng nhập cuối
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Tạo và trả về JWT token
        String roleStr = user.getRole().toString();
        String token = jwtUtil.generateToken(user.getEmail(), roleStr);

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole()
        ));
    }

    public ResponseEntity<?> verifyEmail(String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        User user = userOptional.get();

        // Kiểm tra token có hết hạn chưa
        if (user.getTokenExpiryDate() != null && user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Mã xác thực đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // Cập nhật trạng thái user
        user.setEmailVerified(true);
        user.setStatus(1); // 1 = Active
        user.setVerificationToken(null); // Xóa token sau khi sử dụng
        user.setTokenExpiryDate(null);

        userRepository.save(user);

        return ResponseEntity.ok("Xác thực email thành công! Bây giờ bạn có thể đăng nhập.");
    }

    public ResponseEntity<?> resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy tài khoản với email này");
        }

        User user = userOptional.get();

        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Email này đã được xác thực");
        }

        // Tạo token mới
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken, user.getFullName());
            return ResponseEntity.ok("Email xác thực mới đã được gửi thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể gửi email xác thực: " + e.getMessage());
        }
    }

    /**
     * Đăng xuất người dùng bằng cách vô hiệu hóa token JWT
     *
     * @param request HttpServletRequest chứa token trong header Authorization
     * @return ResponseEntity thông báo đăng xuất thành công
     */
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Vô hiệu hóa token bằng cách thêm vào danh sách đen
            jwtUtil.invalidateToken(token);
            return ResponseEntity.ok("Đăng xuất thành công");
        }

        return ResponseEntity.badRequest().body("Không tìm thấy token xác thực");
    }
}
