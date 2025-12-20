package com.example.backend.service;

import com.example.backend.entity.AuthUser;
import com.example.backend.entity.User;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final JwtService jwtService;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(JwtService jwtService,  AuthUserRepository authUserRepository,  PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(String email, String password, String fullName) throws UserAlreadyExistsException {
        Optional<AuthUser> authUserExists = authUserRepository.findByEmail(email);
        if (authUserExists.isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User newUser = new User();
        newUser.setFullName(fullName);
        AuthUser authUser = AuthUser.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .user(newUser)
                .build();
        authUserRepository.save(authUser);
    }

    public Map<String, String> login(String email, String password) throws BadCredentialsException {
        Optional<AuthUser> authUserExists = authUserRepository.findByEmail(email);
        if (authUserExists.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        AuthUser authUser = authUserExists.get();

        if (!passwordEncoder.matches(password, authUser.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(authUser.getUser().getId().toString());
        String refreshToken = jwtService.generateRefreshToken(authUser.getUser().getId().toString());

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);

        return map;
    }
}
