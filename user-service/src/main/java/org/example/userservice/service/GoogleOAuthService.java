package org.example.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

/**
 * Service for verifying Google OAuth ID tokens and extracting user information.
 * Uses Google's official API client library for secure token verification.
 */
@Service
@Slf4j
public class GoogleOAuthService {

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (googleClientId != null && !googleClientId.isEmpty()) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            log.info("Google OAuth verifier initialized with client ID: {}...",
                    googleClientId.substring(0, Math.min(20, googleClientId.length())));
        } else {
            log.warn("Google OAuth client ID not configured. Google Sign-In will be disabled.");
        }
    }

    /**
     * Verify Google ID token and extract user information.
     * 
     * @param idTokenString The ID token string received from Google Sign-In
     * @return Optional containing GoogleUserInfo if token is valid, empty otherwise
     */
    public Optional<GoogleUserInfo> verifyIdToken(String idTokenString) {
        if (verifier == null) {
            log.error("Google OAuth verifier not initialized. Check GOOGLE_OAUTH_CLIENT_ID configuration.");
            return Optional.empty();
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.warn("Invalid Google ID token - verification failed");
                return Optional.empty();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Verify the token was intended for our application
            String audience = (String) payload.getAudience();
            if (!googleClientId.equals(audience)) {
                log.warn("Token audience mismatch. Expected: {}, Got: {}", googleClientId, audience);
                return Optional.empty();
            }

            GoogleUserInfo userInfo = GoogleUserInfo.builder()
                    .googleId(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(payload.getEmailVerified())
                    .fullName((String) payload.get("name"))
                    .givenName((String) payload.get("given_name"))
                    .familyName((String) payload.get("family_name"))
                    .pictureUrl((String) payload.get("picture"))
                    .locale((String) payload.get("locale"))
                    .build();

            log.info("Successfully verified Google ID token for user: {}", userInfo.getEmail());
            return Optional.of(userInfo);

        } catch (GeneralSecurityException e) {
            log.error("Security exception while verifying Google ID token: {}", e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.error("IO exception while verifying Google ID token: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error verifying Google ID token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Check if Google OAuth is properly configured.
     * 
     * @return true if Google OAuth is enabled and configured
     */
    public boolean isGoogleOAuthEnabled() {
        return verifier != null && googleClientId != null && !googleClientId.isEmpty();
    }

    /**
     * Get the configured Google Client ID (for frontend reference).
     * 
     * @return The Google OAuth Client ID
     */
    public String getGoogleClientId() {
        return googleClientId;
    }
}
