package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Password reset token entity.
 * TTL index automatically deletes expired tokens after 1 hour.
 */
@Document(collection = "password_reset_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    private String email;

    private LocalDateTime createdAt;

    @Indexed(expireAfterSeconds = 3600) // Auto-delete after 1 hour
    private LocalDateTime expiresAt;

    private Boolean used;

    /**
     * Check if the token is valid (not expired and not used)
     */
    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
