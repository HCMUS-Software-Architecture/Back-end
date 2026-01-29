package org.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth authentication.
 * The client sends the Google ID token received from Google Sign-In.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleOAuthRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    /**
     * Optional: Client ID for additional verification.
     * If provided, will be verified against server's configured client ID.
     */
    private String clientId;
}
