package org.example.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Mock email service - logs emails instead of sending.
 * For production, implement actual email sending via SMTP/SendGrid/etc.
 */
@Service
@Slf4j
public class EmailService {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Mock: Send password reset email - only logs
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        log.info("=== PASSWORD RESET EMAIL (Mock) ===");
        log.info("To: {}", toEmail);
        log.info("Reset Link: {}", resetLink);
        log.info("Token: {}", resetToken);
        log.info("===================================");
    }

    /**
     * Mock: Send welcome email - only logs
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        log.info("=== WELCOME EMAIL (Mock) ===");
        log.info("To: {}", toEmail);
        log.info("Welcome, {}!", fullName);
        log.info("============================");
    }

    /**
     * Mock: Send password changed confirmation - only logs
     */
    @Async
    public void sendPasswordChangedEmail(String toEmail) {
        log.info("=== PASSWORD CHANGED EMAIL (Mock) ===");
        log.info("To: {}", toEmail);
        log.info("Your password has been changed.");
        log.info("=====================================");
    }

    /**
     * Mock: Send subscription upgrade confirmation - only logs
     */
    @Async
    public void sendSubscriptionUpgradeEmail(String toEmail, String fullName, String subscriptionType) {
        log.info("=== SUBSCRIPTION UPGRADE EMAIL (Mock) ===");
        log.info("To: {}", toEmail);
        log.info("{} upgraded to {} subscription!", fullName, subscriptionType);
        log.info("=========================================");
    }
}
