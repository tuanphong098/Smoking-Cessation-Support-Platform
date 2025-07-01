package edu.fpt.smokingcessionnew.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String fromEmail;

    @Value("${app.email.dev-mode:true}")
    private boolean devMode;

    public void sendVerificationEmail(String to, String token, String fullName) {
        if (devMode) {
            logger.info("DEV MODE: Simulating email sent to: {} with token: {}", to, token);
            logger.info("Verification URL: {}/api/auth/verify-email?token={}", appUrl, token);
            return;
        }

        try {
            if (javaMailSender == null) {
                logger.error("JavaMailSender not configured. Email not sent.");
                return;
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác nhận đăng ký tài khoản - Ứng dụng Cai thuốc lá");

            // Tạo URL xác thực
            String verificationUrl = appUrl + "/api/auth/verify-email?token=" + token;

            // Sử dụng Thymeleaf để render nội dung email
            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("verificationUrl", verificationUrl);

            String htmlContent = templateEngine.process("verification-email", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            logger.info("Verification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send verification email: {}", e.getMessage());
            // Trong môi trường phát triển/test, không ném exception để không ảnh hưởng đến luồng đăng ký
        } catch (Exception e) {
            logger.error("Unexpected error when sending email: {}", e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String to, String token, String fullName) {
        if (devMode) {
            logger.info("DEV MODE: Simulating password reset email sent to: {} with token: {}", to, token);
            logger.info("Reset URL: {}/reset-password?token={}", appUrl, token);
            return;
        }

        try {
            if (javaMailSender == null) {
                logger.error("JavaMailSender not configured. Email not sent.");
                return;
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu - Ứng dụng Cai thuốc lá");

            String resetUrl = appUrl + "/reset-password?token=" + token;

            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("reset-password-email", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error when sending email: {}", e.getMessage());
        }
    }

    public void sendSimpleEmail(String to, String subject, String content) {
        if (devMode) {
            logger.info("DEV MODE: Simulating email sent to: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("Content: {}", content);
            return;
        }

        try {
            if (javaMailSender == null) {
                logger.error("JavaMailSender not configured. Email not sent.");
                return;
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false); // false = plain text

            javaMailSender.send(message);
            logger.info("Simple email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send simple email: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error when sending simple email: {}", e.getMessage());
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (devMode) {
            logger.info("DEV MODE: Simulating HTML email sent to: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("HTML Content: {}", htmlContent);
            return;
        }

        try {
            if (javaMailSender == null) {
                logger.error("JavaMailSender not configured. Email not sent.");
                return;
            }

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            javaMailSender.send(message);
            logger.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error when sending HTML email: {}", e.getMessage());
        }
    }
}
