package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Phương thức đặt lại mật khẩu cho người dùng (chỉ dành cho Admin)
     * @param userId ID của người dùng cần đặt lại mật khẩu
     * @return ResponseEntity chứa kết quả và mật khẩu tạm thời
     */
    public ResponseEntity<?> resetUserPassword(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng với ID: " + userId);
        }

        User user = userOptional.get();

        // Tạo mật khẩu tạm thời
        String temporaryPassword = generateTemporaryPassword();

        // Mã hóa và lưu mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);

        // Trong môi trường thực tế, gửi email thông báo cho người dùng
        try {
            // Gửi email với mật khẩu tạm thời
            // emailService.sendPasswordResetEmail(user.getEmail(), temporaryPassword, user.getFullName());

            // Trả về thông tin cho admin
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Đã đặt lại mật khẩu cho người dùng thành công");
            result.put("userId", userId);
            result.put("email", user.getEmail());
            result.put("temporaryPassword", temporaryPassword);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đặt lại mật khẩu thất bại: " + e.getMessage());
        }
    }

    /**
     * Tạo mật khẩu tạm thời ngẫu nhiên
     * @return Mật khẩu tạm thời
     */
    private String generateTemporaryPassword() {
        // Các ký tự cho mật khẩu
        String upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerChars = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()_-+=<>?";

        String allChars = upperChars + lowerChars + numbers + specialChars;

        // Tạo mật khẩu với độ dài 10 ký tự
        StringBuilder password = new StringBuilder();

        // Đảm bảo có ít nhất 1 ký tự viết hoa
        password.append(upperChars.charAt((int) (Math.random() * upperChars.length())));

        // Đảm bảo có ít nhất 1 ký tự viết thường
        password.append(lowerChars.charAt((int) (Math.random() * lowerChars.length())));

        // Đảm bảo có ít nhất 1 số
        password.append(numbers.charAt((int) (Math.random() * numbers.length())));

        // Đảm bảo có ít nhất 1 ký tự đặc biệt
        password.append(specialChars.charAt((int) (Math.random() * specialChars.length())));

        // Thêm 6 ký tự ngẫu nhiên nữa
        for (int i = 0; i < 6; i++) {
            password.append(allChars.charAt((int) (Math.random() * allChars.length())));
        }

        // Trộn các ký tự để tránh việc có mẫu cố định
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = (int) (Math.random() * passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return new String(passwordArray);
    }
}
