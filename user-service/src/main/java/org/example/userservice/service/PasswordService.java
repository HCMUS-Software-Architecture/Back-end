package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exception.InvalidTokenException;
import org.example.userservice.model.PasswordResetToken;
import org.example.userservice.model.User;
import org.example.userservice.repository.PasswordResetTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int TOKEN_VALIDITY_HOURS = 1;

    /**
     * Initiate password reset flow.
     * Generates a reset token and sends email.
     * Returns true regardless of whether email exists (security best practice).
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists - just log and return
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Check if user is OAuth-based
        if (user.isOAuthUser()) {
            log.warn("Password reset requested for OAuth user: {}", email);
            // Still don't reveal - could send different email suggesting OAuth login
            return;
        }

        // Delete any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Generate new reset token
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .email(email)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS))
                .used(false)
                .build();

        tokenRepository.save(resetToken);
        log.info("Password reset token created for user: {}", user.getId());

        // Send reset email
        emailService.sendPasswordResetEmail(email, token);
    }

    /**
     * Reset password using a valid token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Attempting password reset with token");

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        // Check if token is still valid
        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Reset token has expired");
        }

        // Find the user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getId());

        // Send confirmation email
        emailService.sendPasswordChangedEmail(user.getEmail());
    }

    /**
     * Validate a reset token without using it.
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> resetToken = tokenRepository.findByTokenAndUsedFalse(token);
        return resetToken.isPresent() && resetToken.get().isValid();
    }
}
