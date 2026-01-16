package com.example.backend.service;

import com.example.backend.dto.TokenResponse;
import com.example.backend.model.RefreshToken;
import com.example.backend.exception.RefreshTokenRevokeException;
import com.example.backend.repository.mongodb.RefreshTokenMongoRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for JwtService
 * 
 * Test Coverage:
 * - Token generation (access & refresh)
 * - Token validation
 * - Token parsing and claims extraction
 * - Refresh token rotation
 * - Token revocation handling
 * 
 * Future-proof for microservices:
 * - Tests are isolated (no external dependencies)
 * - Mocks repository layer (can be replaced with message queues)
 * - Validates JWT structure (portable across services)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Unit Tests")
class JwtServiceTest {

    @Mock
    private RefreshTokenMongoRepository refreshTokenRepository;

    @InjectMocks
    private JwtService jwtService;

    private final String TEST_SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesThatIsAtLeast256BitsLong12345";
    private final String TEST_USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", TEST_SECRET);
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void generateAccessToken_shouldContainUserIdClaim() {
        // When
        String token = jwtService.generateAccessToken(TEST_USER_ID);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        // Verify token structure and claims
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo(TEST_USER_ID);
        assertThat(claims.get("user_id", String.class)).isEqualTo(TEST_USER_ID);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("Should generate refresh token and save to repository")
    void generateRefreshToken_shouldSaveToRepository() {
        // Given
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String token = jwtService.generateRefreshToken(TEST_USER_ID);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        verify(refreshTokenRepository, times(1))
                .save(argThat(refreshToken -> refreshToken.getUserId().equals(TEST_USER_ID) &&
                        refreshToken.getToken().equals(token) &&
                        !refreshToken.getIsRevoked() &&
                        refreshToken.getExpires_at().after(new Date())));
    }

    @Test
    @DisplayName("Should extract user ID from valid token")
    void extractUserId_shouldReturnCorrectUserId() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID);

        // When
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void extractUsername_shouldReturnCorrectUsername() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should validate token successfully with correct username")
    void validateToken_shouldReturnTrueForValidToken() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID);

        // When
        boolean isValid = jwtService.validateToken(token, TEST_USER_ID);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should fail validation with incorrect username")
    void validateToken_shouldReturnFalseForInvalidUsername() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID);

        // When
        boolean isValid = jwtService.validateToken(token, "wrong-user");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should rotate refresh token successfully")
    void getAccessTokenFromRefreshToken_shouldRotateTokens() throws RefreshTokenRevokeException {
        // Given - create initial refresh token
        String initialToken = jwtService.generateRefreshToken(TEST_USER_ID);

        RefreshToken storedToken = RefreshToken.builder()
                .token(initialToken)
                .userId(TEST_USER_ID)
                .isRevoked(false)
                .expires_at(new Date(System.currentTimeMillis() + 86400000))
                .build();

        when(refreshTokenRepository.findByToken(initialToken)).thenReturn(storedToken);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TokenResponse response = jwtService.getAccessTokenFromRefreshToken(initialToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(response.getRefreshToken()).isNotNull().isNotEmpty();

        // Verify old token was revoked (saved with isRevoked=true)
        verify(refreshTokenRepository, atLeast(1))
                .save(argThat(token -> token.getToken().equals(initialToken) && token.getIsRevoked()));

        // Verify new refresh token was created
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when refresh token is already revoked")
    void getAccessTokenFromRefreshToken_shouldThrowExceptionForRevokedToken() {
        // Given
        String oldRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        RefreshToken storedToken = RefreshToken.builder()
                .token(oldRefreshToken)
                .userId(TEST_USER_ID)
                .isRevoked(true) // Already revoked
                .expires_at(new Date(System.currentTimeMillis() + 86400000))
                .build();

        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(storedToken);

        // When & Then
        assertThatThrownBy(() -> jwtService.getAccessTokenFromRefreshToken(oldRefreshToken))
                .isInstanceOf(RefreshTokenRevokeException.class)
                .hasMessageContaining("refresh token is revoke");
    }

    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_shouldRejectMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(malformedToken, TEST_USER_ID))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("Should reject token signed with different secret")
    void validateToken_shouldRejectTokenWithWrongSignature() {
        // Given - create token with different secret
        String differentSecret = "DifferentSecretKeyThatIsAlsoAtLeast256BitsLongForTestingPurpose12345";
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", differentSecret);
        String tokenWithDifferentSecret = jwtService.generateAccessToken(TEST_USER_ID);

        // Reset to original secret
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", TEST_SECRET);

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(tokenWithDifferentSecret, TEST_USER_ID))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
