package edu.fpt.smokingcessionnew.security;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Lớp quản lý danh sách các token JWT đã bị vô hiệu hóa (blacklisted)
 * Khi người dùng đăng xuất, token của họ sẽ được thêm vào danh sách này
 * để tránh việc sử dụng lại token cũ
 */
@Component
public class JwtBlacklist {

    // Lưu trữ các token đã bị vô hiệu hóa và thời gian hết hạn
    private final Set<BlacklistedToken> blacklistedTokens = new HashSet<>();

    /**
     * Thêm token vào danh sách đen
     *
     * @param token Token cần vô hiệu hóa
     * @param expiryDate Thời gian hết hạn của token
     */
    public void addToBlacklist(String token, Date expiryDate) {
        // Cắt bỏ phần prefix "Bearer " nếu có
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        blacklistedTokens.add(new BlacklistedToken(token, expiryDate));

        // Xóa các token hết hạn để tiết kiệm bộ nhớ
        removeExpiredTokens();
    }

    /**
     * Kiểm tra xem token có trong danh sách đen hay không
     *
     * @param token Token cần kiểm tra
     * @return true nếu token nằm trong danh sách đen
     */
    public boolean isBlacklisted(String token) {
        // Cắt bỏ phần prefix "Bearer " nếu có
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        final String finalToken = token;
        return blacklistedTokens.stream()
            .anyMatch(t -> t.getToken().equals(finalToken) && t.getExpiryDate().after(new Date()));
    }

    /**
     * Xóa các token đã hết hạn khỏi danh sách
     */
    private void removeExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.removeIf(token -> token.getExpiryDate().before(now));
    }

    /**
     * Lớp nội bộ để lưu thông tin token bị đưa vào blacklist
     */
    private static class BlacklistedToken {
        private final String token;
        private final Date expiryDate;

        public BlacklistedToken(String token, Date expiryDate) {
            this.token = token;
            this.expiryDate = expiryDate;
        }

        public String getToken() {
            return token;
        }

        public Date getExpiryDate() {
            return expiryDate;
        }
    }
}
