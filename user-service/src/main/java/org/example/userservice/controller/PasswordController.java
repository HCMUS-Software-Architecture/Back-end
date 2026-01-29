package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.ForgotPasswordRequest;
import org.example.userservice.dto.ResetPasswordRequest;
import org.example.userservice.service.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password", description = "Password recovery APIs")
public class PasswordController {
    private final PasswordService passwordService;

    @PostMapping("/forgot")
    @Operation(summary = "Request password reset email",
            description = "Sends a password reset link to the provided email if it exists. " +
                    "Always returns success for security reasons.")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        
        passwordService.initiatePasswordReset(request.getEmail());
        
        // Always return success - don't reveal if email exists
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent."
        ));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password with token",
            description = "Resets the user's password using the token from the reset email.")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(Map.of(
                "message", "Password has been reset successfully. You can now login with your new password."
        ));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate reset token",
            description = "Checks if a password reset token is valid (not expired or used).")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        boolean isValid = passwordService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
