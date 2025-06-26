package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_1')") // Chỉ admin mới có quyền xem tất cả users
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Integer userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@RequestBody User user, @PathVariable Integer userId) {
        return userService.updateUser(user, userId);
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable Integer userId,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        return userService.changePassword(userId, currentPassword, newPassword);
    }

    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Integer userId) {
        return userService.deactivateUser(userId);
    }

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ROLE_1')") // Chỉ admin mới có quyền kích hoạt tài khoản
    public ResponseEntity<?> activateUser(@PathVariable Integer userId) {
        return userService.activateUser(userId);
    }

    @PutMapping("/{userId}/change-role")
    @PreAuthorize("hasRole('ROLE_1')") // Chỉ admin mới có quyền thay đổi vai trò
    public ResponseEntity<?> changeUserRole(
            @PathVariable Integer userId,
            @RequestParam Integer newRole) {
        return userService.changeUserRole(userId, newRole);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        return userService.generatePasswordResetToken(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        return userService.resetPassword(token, newPassword);
    }

    /**
     * API cho phép admin đặt lại mật khẩu cho người dùng và trả về mật khẩu tạm thời
     * @param userId ID của người dùng cần đặt lại mật khẩu
     * @return Thông tin kết quả và mật khẩu tạm thời
     */
    @PostMapping("/{userId}/admin-reset-password")
    @PreAuthorize("hasRole('ROLE_1')") // Chỉ admin mới có quyền thực hiện chức năng này
    public ResponseEntity<?> adminResetUserPassword(@PathVariable Integer userId) {
        return userService.resetUserPassword(userId);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile(@RequestParam String email) {
        // Thực tế thường lấy email từ token JWT thông qua Principal hoặc SecurityContext
        // Nhưng ở đây đơn giản hóa bằng cách nhận email qua parameter
        return userService.getUserByEmail(email);
    }
}
