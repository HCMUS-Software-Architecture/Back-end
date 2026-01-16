package com.example.backend.integration;

import com.example.backend.dto.TokenResponse;
import com.example.backend.dto.UserDto;
import com.example.backend.exception.RefreshTokenNotExist;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.repository.mongodb.RefreshTokenMongoRepository;
import com.example.backend.repository.mongodb.UserMongoRepository;
import com.example.backend.service.AuthService;
import com.example.backend.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Tests for Authentication Flow
 * 
 * Tests the complete authentication workflow including:
 * - User registration
 * - Login with JWT token generation
 * - Token validation
 * - Token refresh
 * - Logout and token revocation
 * 
 * Uses in-memory MongoDB (Flapdoodle) for testing
 * 
 * Future-proof for microservices:
 * - Tests end-to-end auth flow (portable to Auth Service)
 * - Token format validation (consistent across services)
 * - Refresh token rotation pattern (security best practice)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Authentication Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserMongoRepository userRepository;

    @Autowired
    private RefreshTokenMongoRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FULL_NAME = "Integration Test User";
    private static String registeredUserId;
    private static String accessToken;
    private static String refreshToken;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use embedded MongoDB for testing
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017");
        registry.add("spring.data.mongodb.database", () -> "trading_test");

        // Disable Redis for unit tests (use in-memory cache instead)
        registry.add("spring.cache.type", () -> "simple");

        // Use test JWT secret
        registry.add("token.secret",
                () -> "TestSecretKeyForIntegrationTestingThatIsAtLeast256BitsLongAndSecure1234567890");
    }

    @BeforeEach
    void setUp() {
        // Clean up test data before each test
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
        refreshTokenRepository.deleteAll();
    }

    @AfterAll
    static void tearDown(@Autowired UserMongoRepository userRepo,
            @Autowired RefreshTokenMongoRepository tokenRepo) {
        // Clean up after all tests
        userRepo.findByEmail(TEST_EMAIL).ifPresent(userRepo::delete);
        tokenRepo.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should register new user and persist to database")
    void testUserRegistration() throws UserAlreadyExistsException {
        // When
        UserDto result = authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getFullName()).isEqualTo(TEST_FULL_NAME);
        assertThat(result.getId()).isNotNull();

        // Verify user is persisted
        Optional<User> savedUser = userRepository.findByEmail(TEST_EMAIL);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.get().getPassword())).isTrue();

        registeredUserId = result.getId();
    }

    @Test
    @Order(2)
    @DisplayName("Should prevent duplicate user registration")
    void testDuplicateUserRegistration() throws UserAlreadyExistsException {
        // Given - register user first
        authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        // When & Then - attempt to register again
        assertThatThrownBy(() -> authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @Order(3)
    @DisplayName("Should login successfully and generate JWT tokens")
    void testSuccessfulLogin() throws UserAlreadyExistsException {
        // Given - register user first
        UserDto user = authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        // When
        TokenResponse tokens = authService.login(TEST_EMAIL, TEST_PASSWORD);

        // Then
        assertThat(tokens).isNotNull();
        assertThat(tokens.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(tokens.getRefreshToken()).isNotNull().isNotEmpty();

        // Verify tokens are valid
        String userId = jwtService.extractUserId(tokens.getAccessToken());
        assertThat(userId).isEqualTo(user.getId());

        boolean isValidAccess = jwtService.validateToken(tokens.getAccessToken(), userId);
        assertThat(isValidAccess).isTrue();

        // Verify refresh token is persisted
        Optional<RefreshToken> savedRefreshToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(tokens.getRefreshToken());
        assertThat(savedRefreshToken).isPresent();
        assertThat(savedRefreshToken.get().getUserId()).isEqualTo(user.getId());

        accessToken = tokens.getAccessToken();
        refreshToken = tokens.getRefreshToken();
        registeredUserId = user.getId();
    }

    @Test
    @Order(4)
    @DisplayName("Should reject login with incorrect password")
    void testLoginWithIncorrectPassword() throws UserAlreadyExistsException {
        // Given
        authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);

        // When & Then
        assertThatThrownBy(() -> authService.login(TEST_EMAIL, "WrongPassword123!"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @Order(5)
    @DisplayName("Should reject login with non-existent email")
    void testLoginWithNonExistentEmail() {
        // When & Then
        assertThatThrownBy(() -> authService.login("nonexistent@test.com", TEST_PASSWORD))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @Order(6)
    @DisplayName("Should refresh tokens successfully")
    void testTokenRefresh() throws Exception {
        // Given - register and login
        UserDto user = authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
        TokenResponse initialTokens = authService.login(TEST_EMAIL, TEST_PASSWORD);

        // When - refresh tokens
        TokenResponse newTokens = jwtService.getAccessTokenFromRefreshToken(initialTokens.getRefreshToken());

        // Then
        assertThat(newTokens).isNotNull();
        assertThat(newTokens.getAccessToken()).isNotNull().isNotEqualTo(initialTokens.getAccessToken());
        assertThat(newTokens.getRefreshToken()).isNotNull().isNotEqualTo(initialTokens.getRefreshToken());

        // Verify old refresh token is revoked
        RefreshToken oldToken = refreshTokenRepository.findByToken(initialTokens.getRefreshToken());
        assertThat(oldToken).isNotNull();
        assertThat(oldToken.getIsRevoked()).isTrue();

        // Verify new refresh token is active
        Optional<RefreshToken> newToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(newTokens.getRefreshToken());
        assertThat(newToken).isPresent();
        assertThat(newToken.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    @Order(7)
    @DisplayName("Should logout and revoke refresh token")
    void testLogout() throws Exception {
        // Given - register and login
        authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
        TokenResponse tokens = authService.login(TEST_EMAIL, TEST_PASSWORD);

        // Verify token is active before logout
        Optional<RefreshToken> activeToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(tokens.getRefreshToken());
        assertThat(activeToken).isPresent();

        // When - logout
        authService.logout(tokens.getRefreshToken());

        // Then - verify token is revoked
        Optional<RefreshToken> revokedToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(tokens.getRefreshToken());
        assertThat(revokedToken).isEmpty();

        RefreshToken tokenAfterLogout = refreshTokenRepository.findByToken(tokens.getRefreshToken());
        assertThat(tokenAfterLogout).isNotNull();
        assertThat(tokenAfterLogout.getIsRevoked()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Should reject logout with non-existent token")
    void testLogoutWithNonExistentToken() {
        // When & Then
        assertThatThrownBy(() -> authService.logout("non-existent-token"))
                .isInstanceOf(RefreshTokenNotExist.class)
                .hasMessageContaining("Refresh token does not exist");
    }

    @Test
    @Order(9)
    @DisplayName("Should validate active refresh token")
    void testRefreshTokenValidation() throws UserAlreadyExistsException {
        // Given
        authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
        TokenResponse tokens = authService.login(TEST_EMAIL, TEST_PASSWORD);

        // When
        Optional<String> userId = authService.validateRefreshToken(tokens.getRefreshToken());

        // Then
        assertThat(userId).isPresent();
    }

    @Test
    @Order(10)
    @DisplayName("Should reject revoked refresh token")
    void testRevokedRefreshTokenValidation() throws Exception {
        // Given
        authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
        TokenResponse tokens = authService.login(TEST_EMAIL, TEST_PASSWORD);
        authService.logout(tokens.getRefreshToken());

        // When
        Optional<String> userId = authService.validateRefreshToken(tokens.getRefreshToken());

        // Then
        assertThat(userId).isEmpty();
    }

    @Test
    @Order(11)
    @DisplayName("Should handle complete authentication flow")
    void testCompleteAuthenticationFlow() throws Exception {
        // Step 1: Register
        UserDto user = authService.registerUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME);
        assertThat(user.getId()).isNotNull();

        // Step 2: Login
        TokenResponse loginTokens = authService.login(TEST_EMAIL, TEST_PASSWORD);
        assertThat(loginTokens.getAccessToken()).isNotNull();
        assertThat(loginTokens.getRefreshToken()).isNotNull();

        // Step 3: Validate access token
        String userId = jwtService.extractUserId(loginTokens.getAccessToken());
        boolean isAccessTokenValid = jwtService.validateToken(loginTokens.getAccessToken(), userId);
        assertThat(isAccessTokenValid).isTrue();

        // Step 4: Validate refresh token
        Optional<String> validatedUserId = authService.validateRefreshToken(loginTokens.getRefreshToken());
        assertThat(validatedUserId).isPresent();
        assertThat(validatedUserId.get()).isEqualTo(user.getId());

        // Step 5: Refresh tokens
        TokenResponse refreshedTokens = jwtService.getAccessTokenFromRefreshToken(loginTokens.getRefreshToken());
        assertThat(refreshedTokens.getAccessToken()).isNotEqualTo(loginTokens.getAccessToken());

        // Step 6: Logout
        authService.logout(refreshedTokens.getRefreshToken());

        // Step 7: Verify token is revoked
        Optional<String> userIdAfterLogout = authService.validateRefreshToken(refreshedTokens.getRefreshToken());
        assertThat(userIdAfterLogout).isEmpty();
    }
}
