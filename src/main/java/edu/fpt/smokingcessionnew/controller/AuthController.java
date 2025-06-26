package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.dto.AuthResponse;
import edu.fpt.smokingcessionnew.dto.LoginRequest;
import edu.fpt.smokingcessionnew.dto.RegisterRequest;
import edu.fpt.smokingcessionnew.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới", description = "Đăng ký tài khoản mới với email, mật khẩu và vai trò (Member hoặc Coach)")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        // Kiểm tra lỗi validation
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getAllErrors().forEach(error -> {
                String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.BAD_REQUEST.value());
            response.put("message", "Dữ liệu không hợp lệ");
            response.put("errors", errors);

            return ResponseEntity.badRequest().body(response);
        }

        // Kiểm tra password và confirmPassword
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        // Gọi AuthService để xử lý đăng ký
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Đăng nhập với email, mật khẩu và vai trò")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        // Kiểm tra lỗi validation
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getAllErrors().forEach(error -> {
                String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.BAD_REQUEST.value());
            response.put("message", "Dữ liệu đăng nhập không hợp lệ");
            response.put("errors", errors);

            return ResponseEntity.badRequest().body(response);
        }

        // Gọi AuthService để xử lý đăng nhập
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Đăng xuất khỏi hệ thống, vô hiệu hóa token JWT",
              security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> logout(HttpServletRequest request, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Trích xuất token từ Authorization header và truyền vào service
        return authService.logout(request, authHeader, null);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Xác thực email", description = "Xác thực email thông qua token được gửi đến email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/resend-verification-email")
    @Operation(summary = "Gửi lại email xác thực", description = "Gửi lại email xác thực cho tài khoản đã đăng ký")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam("email") String email) {
        return authService.resendVerificationEmail(email);
    }

    // Xử lý ngoại lệ validation khi không sử dụng BindingResult
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Dữ liệu không hợp lệ");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }
}