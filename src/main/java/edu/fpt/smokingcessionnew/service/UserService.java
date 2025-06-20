package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public ResponseEntity<?> getUserById(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }
        return ResponseEntity.ok(userOptional.get());
    }

    public ResponseEntity<?> updateUser(User user, Integer userId) {
        Optional<User> existingUserOptional = userRepository.findById(userId);
        if (existingUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        User existingUser = existingUserOptional.get();

        // Cập nhật thông tin cá nhân
        if (user.getFullName() != null) {
            existingUser.setFullName(user.getFullName());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(user.getDateOfBirth());
        }
        if (user.getGender() != null) {
            existingUser.setGender(user.getGender());
        }

        // Không cho phép thay đổi email hoặc role thông qua API này

        User updatedUser = userRepository.save(existingUser);
        return ResponseEntity.ok(updatedUser);
    }

    public ResponseEntity<?> changePassword(Integer userId, String currentPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        User user = userOptional.get();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Thay đổi mật khẩu thành công");
    }

    public ResponseEntity<?> deactivateUser(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        User user = userOptional.get();
        user.setStatus(0); // 0 = Inactive
        userRepository.save(user);

        return ResponseEntity.ok("Vô hiệu hóa tài khoản thành công");
    }

    public ResponseEntity<?> activateUser(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        User user = userOptional.get();
        user.setStatus(1); // 1 = Active
        userRepository.save(user);

        return ResponseEntity.ok("Kích hoạt tài khoản thành công");
    }

    public ResponseEntity<?> changeUserRole(Integer userId, Integer newRole) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        // Kiểm tra role hợp lệ (1 = Admin, 2 = Member, 3 = Coach)
        if (newRole != 1 && newRole != 2 && newRole != 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role không hợp lệ");
        }

        User user = userOptional.get();
        user.setRole(newRole);
        userRepository.save(user);

        return ResponseEntity.ok("Thay đổi quyền người dùng thành công");
    }

    // Phương thức giúp tạo mã xác thực để reset password
    public ResponseEntity<?> generatePasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email không tồn tại trong hệ thống");
        }

        User user = userOptional.get();
        String resetToken = generateRandomToken();
        user.setVerificationToken(resetToken);
        userRepository.save(user);

        // Trong thực tế, bạn sẽ gửi email với token này
        // Nhưng trong demo này chúng ta chỉ trả về token
        return ResponseEntity.ok("Mã xác thực đã được gửi đến email của bạn");
    }

    // Phương thức reset password với token
    public ResponseEntity<?> resetPassword(String token, String newPassword) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mã xác thực không hợp lệ");
        }

        User user = userOptional.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null); // Xóa token sau khi sử dụng
        userRepository.save(user);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }

    // Sinh mã token ngẫu nhiên
    private String generateRandomToken() {
        return java.util.UUID.randomUUID().toString();
    }

    // Phương thức tìm user theo email
    public ResponseEntity<?> getUserByEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng với email này");
        }
        return ResponseEntity.ok(userOptional.get());
    }
}
