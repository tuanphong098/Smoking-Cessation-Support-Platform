package edu.fpt.smokingcessionnew.config;

import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lớp khởi tạo tài khoản admin khi ứng dụng khởi động
 * Sẽ tự động tạo tài khoản admin nếu chưa tồn tại
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@smokingcessation.com}")
    private String adminEmail;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    @Value("${admin.fullname:System Administrator}")
    private String adminFullName;

    @Override
    @Transactional
    public void run(String... args) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (!userRepository.existsByEmail(adminEmail)) {
            logger.info("Tạo tài khoản Admin mặc định với email: {}", adminEmail);

            User adminUser = new User();
            adminUser.setEmail(adminEmail);
            adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminUser.setFullName(adminFullName);
            adminUser.setRole(1); // 1 = Admin role
            adminUser.setStatus(1); // 1 = Active
            adminUser.setCreatedDate(LocalDateTime.now());
            adminUser.setEmailVerified(true); // Admin không cần xác thực email

            userRepository.save(adminUser);

            logger.info("Đã tạo tài khoản Admin thành công!");
        } else {
            logger.info("Tài khoản Admin đã tồn tại, bỏ qua khởi tạo.");
        }
    }
}
