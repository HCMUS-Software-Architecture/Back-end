package com.example.backend.service;

import com.example.backend.dto.TokenResponse;
import com.example.backend.dto.UserDto;
import com.example.backend.exception.RefreshTokenNotExist;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.repository.mongodb.RefreshTokenMongoRepository;
import com.example.backend.repository.mongodb.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService
 * 
 * Test Coverage:
 * - User registration (success & duplicate)
 * - User login (success & failures)
 * - Logout and token revocation
 * - Token validation workflow
 * 
 * Future-proof for microservices:
 * - Tests business logic independent of infrastructure
 * - Repository interactions can be replaced with events/messages
 * - Authentication flow remains consistent across services
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

        @Mock
        private JwtService jwtService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private RefreshTokenMongoRepository refreshTokenMongoRepository;

        @Mock
        private UserMongoRepository userMongoRepository;

        @InjectMocks
        private AuthService authService;

        private User testUser;
        private final String TEST_EMAIL = "test@example.com";
        private final String TEST_PASSWORD = "password123";
        private final String TEST_FULL_NAME = "Test User";
        private final String TEST_USER_ID = "user-123";
        private final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPassword";

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(TEST_USER_ID)
                                .email(TEST_EMAIL)
                                .password(TEST_ENCODED_PASSWORD)
                                .fullName(TEST_FULL_NAME)
                                .build();
        }

        @Test
        @DisplayName("Should register new user successfully")
        void registerUser_shouldCreateNewUser() throws UserAlreadyExistsException {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
                when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
                when(userMongoRepository.save(any(User.class))).thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.setId(TEST_USER_ID);
                        return user;
                });

                // When
                UserDto result = authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
                assertThat(result.getFullName()).isEqualTo(TEST_FULL_NAME);
                assertThat(result.getId()).isEqualTo(TEST_USER_ID);

                verify(userMongoRepository, times(1)).save(argThat(user -> user.getEmail().equals(TEST_EMAIL) &&
                                user.getPassword().equals(TEST_ENCODED_PASSWORD) &&
                                user.getFullName().equals(TEST_FULL_NAME)));
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate email")
        void registerUser_shouldThrowExceptionForDuplicateEmail() {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

                // When & Then
                assertThatThrownBy(() -> authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME))
                                .isInstanceOf(UserAlreadyExistsException.class)
                                .hasMessageContaining("Email already exists");

                verify(userMongoRepository, never()).save(any(User.class));
                verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should login successfully with correct credentials")
        void login_shouldReturnTokensForValidCredentials() {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
                when(jwtService.generateAccessToken(TEST_USER_ID)).thenReturn("access-token");
                when(jwtService.generateRefreshToken(TEST_USER_ID)).thenReturn("refresh-token");

                // When
                TokenResponse result = authService.login(TEST_EMAIL, TEST_PASSWORD);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getAccessToken()).isEqualTo("access-token");
                assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

                verify(jwtService, times(1)).generateAccessToken(TEST_USER_ID);
                verify(jwtService, times(1)).generateRefreshToken(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should throw exception for non-existent user during login")
        void login_shouldThrowExceptionForNonExistentUser() {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> authService.login(TEST_EMAIL, TEST_PASSWORD))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessageContaining("Invalid email or password");

                verify(passwordEncoder, never()).matches(anyString(), anyString());
                verify(jwtService, never()).generateAccessToken(anyString());
        }

        @Test
        @DisplayName("Should throw exception for incorrect password during login")
        void login_shouldThrowExceptionForIncorrectPassword() {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(false);

                // When & Then
                assertThatThrownBy(() -> authService.login(TEST_EMAIL, TEST_PASSWORD))
                                .isInstanceOf(BadCredentialsException.class)
                                .hasMessageContaining("Invalid email or password");

                verify(jwtService, never()).generateAccessToken(anyString());
                verify(jwtService, never()).generateRefreshToken(anyString());
        }

        @Test
        @DisplayName("Should logout successfully and revoke refresh token")
        void logout_shouldRevokeRefreshToken() throws RefreshTokenNotExist {
                // Given
                String refreshTokenString = "valid-refresh-token";
                RefreshToken refreshToken = RefreshToken.builder()
                                .id("token-id")
                                .token(refreshTokenString)
                                .userId(TEST_USER_ID)
                                .isRevoked(false)
                                .expires_at(new Date(System.currentTimeMillis() + 86400000))
                                .build();

                when(refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(refreshTokenString))
                                .thenReturn(Optional.of(refreshToken));
                when(refreshTokenMongoRepository.save(any(RefreshToken.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // When
                authService.logout(refreshTokenString);

                // Then
                verify(refreshTokenMongoRepository, times(1))
                                .save(argThat(token -> token.getToken().equals(refreshTokenString) &&
                                                token.getIsRevoked()));
        }

        @Test
        @DisplayName("Should throw exception when logging out with non-existent token")
        void logout_shouldThrowExceptionForNonExistentToken() {
                // Given
                String invalidToken = "invalid-token";
                when(refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(invalidToken))
                                .thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> authService.logout(invalidToken))
                                .isInstanceOf(RefreshTokenNotExist.class)
                                .hasMessageContaining("Refresh token does not exist");

                verify(refreshTokenMongoRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should validate active refresh token successfully")
        void validateRefreshToken_shouldReturnTrueForActiveToken() {
                // Given
                String validToken = "valid-token";
                RefreshToken refreshToken = RefreshToken.builder()
                                .token(validToken)
                                .userId(TEST_USER_ID)
                                .isRevoked(false)
                                .expires_at(new Date(System.currentTimeMillis() + 86400000))
                                .build();

                when(refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(validToken))
                                .thenReturn(Optional.of(refreshToken));

                // When
                Optional<String> userId = authService.validateRefreshToken(validToken);

                // Then
                assertThat(userId).isPresent();
                assertThat(userId.get()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return false for revoked refresh token")
        void validateRefreshToken_shouldReturnFalseForRevokedToken() {
                // Given
                String revokedToken = "revoked-token";
                when(refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(revokedToken))
                                .thenReturn(Optional.empty());

                // When
                Optional<String> userId = authService.validateRefreshToken(revokedToken);

                // Then
                assertThat(userId).isEmpty();
        }

        @Test
        @DisplayName("Should encode password before saving user")
        void registerUser_shouldEncodePasswordBeforeSaving() throws UserAlreadyExistsException {
                // Given
                when(userMongoRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
                when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
                when(userMongoRepository.save(any(User.class))).thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.setId(TEST_USER_ID);
                        return user;
                });

                // When
                authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

                // Then
                verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
                verify(userMongoRepository, times(1))
                                .save(argThat(user -> user.getPassword().equals(TEST_ENCODED_PASSWORD) &&
                                                !user.getPassword().equals(TEST_PASSWORD)));
        }
}
