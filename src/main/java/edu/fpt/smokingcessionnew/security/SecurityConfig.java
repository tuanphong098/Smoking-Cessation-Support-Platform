package edu.fpt.smokingcessionnew.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Cho phép tất cả các CORS request
            .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()))
            // Vô hiệu hóa CSRF bảo vệ
            .csrf(AbstractHttpConfigurer::disable)
            // Đặt SessionCreationPolicy là STATELESS vì sử dụng JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Cấu hình quyền truy cập cho mọi yêu cầu
            .authorizeHttpRequests(authz -> authz
                // Cho phép truy cập không cần xác thực cho các API công khai
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/verify-email",
                    "/api/auth/resend-verification-email",
                    "/api/users/forgot-password",  // Thêm endpoint forgot password
                    "/api/users/reset-password",   // Thêm endpoint reset password
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/api/test/token-info",
                    "/api/payment/packages",
                    "/api/payment-debug/**",
                    "/api/payment/vnpay-return",
                    "/api/cors-test/**",
                    "/test/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/*.html",
                    "/favicon.ico",
                    "/error",
                    "/actuator/health",
                    "/",
                    "/index.html",
                    "/webjars/**"
                ).permitAll()
                // Bắt buộc xác thực cho tất cả các request khác
                .anyRequest().authenticated()
            )
            // Áp dụng filter JWT trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép tất cả origins - bao gồm cả ngrok và localhost
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Thêm tất cả các HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // Cho phép tất cả headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Expose các headers cần thiết
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count"));

        // Cho phép credentials
        configuration.setAllowCredentials(true);

        // Cache preflight request trong 1 giờ
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
