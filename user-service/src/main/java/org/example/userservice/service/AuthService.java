package org.example.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.TokenResponse;
import org.example.userservice.dto.UserDto;
import org.example.userservice.exception.RefreshTokenNotExist;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.model.RefreshToken;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.repository.RefreshTokenRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenMongoRepository;
    private final UserRepository userMongoRepository;

    @Autowired
    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenMongoRepository,
            UserRepository userMongoRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenMongoRepository = refreshTokenMongoRepository;
        this.userMongoRepository = userMongoRepository;
    }

    public UserDto registerUser(String email, String password, String fullName, SubscriptionType subscriptionType)
            throws UserAlreadyExistsException {
        Optional<User> userOption = userMongoRepository.findByEmail(email);
        if (userOption.isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        // Default to REGULAR if not specified
        SubscriptionType effectiveType = subscriptionType != null ? subscriptionType : SubscriptionType.REGULAR;

        User newUser = User.builder()
                .email(email)
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .subscriptionType(effectiveType)
                .emailVerified(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        // For VIP registration, set indefinite subscription (no end date)
        if (effectiveType == SubscriptionType.VIP) {
            newUser.setSubscriptionStartDate(LocalDateTime.now());
            // If need end date
            // newUser.setSubscriptionEndDate(LocalDateTime.now().getMonth().plus(1));
        }

        // Sync roles with subscription (adds USER role, and VIP if applicable)
        newUser.syncRolesWithSubscription();
        userMongoRepository.save(newUser);

        UserDto userDto = new UserDto();
        userDto.setEmail(email);
        userDto.setFullName(fullName);
        userDto.setId(newUser.getId());
        userDto.setSubscriptionType(newUser.getSubscriptionType());
        userDto.setEmailVerified(newUser.getEmailVerified());
        userDto.setCreatedAt(newUser.getCreatedAt());
        return userDto;
    }

    public TokenResponse login(String email, String password) throws BadCredentialsException {
        Optional<User> authUserExists = userMongoRepository.findByEmail(email);
        if (authUserExists.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User authUser = authUserExists.get();

        if (!passwordEncoder.matches(password, authUser.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(authUser.getId());
        String refreshToken = jwtService.generateRefreshToken(authUser.getId());

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setRefreshToken(refreshToken);

        return tokenResponse;
    }

    public void logout(String oldRefreshToken) throws RefreshTokenNotExist {
        Optional<RefreshToken> token = refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(oldRefreshToken);

        if (token.isPresent()) {
            RefreshToken refreshToken = token.get();
            refreshToken.setIsRevoked(true);
            refreshTokenMongoRepository.save(refreshToken);
        } else {
            throw new RefreshTokenNotExist("Refresh token does not exist");
        }
    }

    /**
     * Validate refresh token - MongoDB-based validation
     *
     * @return userId if token is valid
     */
    public Optional<String> validateRefreshToken(String token) {
        // Validate via MongoDB
        Optional<RefreshToken> dbToken = refreshTokenMongoRepository.findByTokenAndIsRevokedFalse(token);
        if (dbToken.isPresent()) {
            log.debug("Token validated via MongoDB");
            return Optional.of(dbToken.get().getUserId());
        }

        return Optional.empty();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteTokenPeriodic() throws Exception {
        refreshTokenMongoRepository.removeByIsRevoked(true);
    }
}
