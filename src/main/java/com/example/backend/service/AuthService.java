package com.example.backend.service;

import com.example.backend.dto.TokenResponse;
import com.example.backend.dto.UserDto;
import com.example.backend.model.RefreshToken;
import com.example.backend.exception.RefreshTokenNotExist;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.model.User;
import com.example.backend.repository.mongodb.RefreshTokenMongoRepository;
import com.example.backend.repository.mongodb.UserMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenMongoRepository refreshTokenMongoRepository;
    private final UserMongoRepository userMongoRepository;

    @Autowired
    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder,
                       RefreshTokenMongoRepository refreshTokenMongoRepository,
                       UserMongoRepository userMongoRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenMongoRepository = refreshTokenMongoRepository;
        this.userMongoRepository = userMongoRepository;
    }

    public UserDto registerUser(String email, String password, String fullName) throws UserAlreadyExistsException {
        Optional<User> userOption = userMongoRepository.findByEmail(email);
        if (userOption.isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User newUser = User.builder()
                        .email(email)
                        .fullName(fullName)
                        .password(passwordEncoder.encode(password))
                        .build();
        userMongoRepository.save(newUser);

        UserDto userDto = new UserDto();
        userDto.setEmail(email);
        userDto.setFullName(fullName);
        userDto.setId(newUser.getId());
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

        if(token.isPresent()) {
            RefreshToken refreshToken = token.get();
            refreshToken.setIsRevoked(true);
            refreshTokenMongoRepository.save(refreshToken);
        }
        else {
            throw new RefreshTokenNotExist("Refresh token not exist");
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteTokenPeriodic() throws Exception {
        refreshTokenMongoRepository.removeByIsRevoked(true);
    }
}
