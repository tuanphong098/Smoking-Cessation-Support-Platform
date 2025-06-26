package edu.fpt.smokingcessionnew.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JWTUtil {

    // Nên lưu secret key trong application.properties hoặc environment variables
    @Value("${jwt.secret:defaultsecretkeywhichshouldbelongenoughforhs512}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long expiration;

    @Value("${jwt.remember-me-expiration:2592000000}") // 30 days in milliseconds
    private long rememberMeExpiration;

    private SecretKey signingKey;

    @Autowired
    private JwtBlacklist jwtBlacklist;

    // Tạo key cho việc ký JWT
    private SecretKey getSigningKey() {
        if (signingKey == null) {
            // Nếu secret có độ dài >= 64 bytes (512 bits), sử dụng nó
            // Nếu không, tạo key mới an toàn cho HS512
            if (secret.length() >= 64) {
                signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            } else {
                // Tạo key đủ mạnh cho HS512
                signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
                // Lưu lại encoded key để có thể sử dụng lại nếu cần
                String encodedKey = Base64.getEncoder().encodeToString(signingKey.getEncoded());
                System.out.println("Generated new secure key for JWT: " + encodedKey);
                System.out.println("Consider adding this key to your application.properties as jwt.secret");
            }
        }
        return signingKey;
    }

    // Tạo token từ thông tin user
    public String generateToken(String email, String role) {
        return generateToken(email, role, false);
    }

    // Tạo token với option remember me
    public String generateToken(String email, String role, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        long tokenExpiration = rememberMe ? rememberMeExpiration : expiration;
        return createToken(claims, email, tokenExpiration);
    }

    // Tạo token với các claims và thời gian hết hạn
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Lấy email từ token
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Lấy ngày hết hạn từ token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Lấy claim từ token
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Lấy tất cả claims từ token
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Kiểm tra token còn hạn không
    private Boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Thêm token vào danh sách đen (blacklist) khi đăng xuất
    public void invalidateToken(String token) {
        try {
            Date expiryDate = getExpirationDateFromToken(token);
            jwtBlacklist.addToBlacklist(token, expiryDate);
        } catch (Exception e) {
            // Token có thể không hợp lệ, nhưng vẫn thêm vào blacklist để đảm bảo
            jwtBlacklist.addToBlacklist(token, new Date(System.currentTimeMillis() + expiration));
        }
    }

    // Xác thực token
    public Boolean validateToken(String token) {
        try {
            // Kiểm tra token có trong danh sách đen không
            if (jwtBlacklist.isBlacklisted(token)) {
                return false;
            }
            // Kiểm tra token có hết hạn không
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
