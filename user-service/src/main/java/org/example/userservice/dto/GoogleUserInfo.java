package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing user information extracted from Google ID token.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleUserInfo {

    /**
     * Google's unique user ID (subject claim).
     */
    private String googleId;

    /**
     * User's email address from Google account.
     */
    private String email;

    /**
     * Whether Google has verified the email.
     */
    private Boolean emailVerified;

    /**
     * User's full name from Google profile.
     */
    private String fullName;

    /**
     * User's given/first name.
     */
    private String givenName;

    /**
     * User's family/last name.
     */
    private String familyName;

    /**
     * URL to user's Google profile picture.
     */
    private String pictureUrl;

    /**
     * Locale/language preference.
     */
    private String locale;
}
