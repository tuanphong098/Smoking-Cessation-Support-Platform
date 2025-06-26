package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.security.JWTUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JWTUtil jwtUtil;

    @GetMapping("/token-info")
    @Operation(summary = "Kiểm tra thông tin token", description = "Hiển thị thông tin của token JWT hiện tại")
    public ResponseEntity<?> getTokenInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Loại bỏ "Bearer "
            Claims claims = jwtUtil.getAllClaimsFromToken(token);

            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Tính toán thời hạn token (theo phút)
            long durationMinutes = (expiration.getTime() - issuedAt.getTime()) / (60 * 1000);

            // Tính toán thời gian còn lại (theo phút)
            long remainingMinutes = (expiration.getTime() - new Date().getTime()) / (60 * 1000);

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("email", email);
            tokenInfo.put("role", role);
            tokenInfo.put("issuedAt", issuedAt);
            tokenInfo.put("expiration", expiration);
            tokenInfo.put("tokenDurationMinutes", durationMinutes);
            tokenInfo.put("remainingMinutes", remainingMinutes);
            tokenInfo.put("isLongTermToken", durationMinutes > 24 * 60); // Nếu token có thời hạn > 1 ngày

            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token không hợp lệ: " + e.getMessage());
        }
    }
}
