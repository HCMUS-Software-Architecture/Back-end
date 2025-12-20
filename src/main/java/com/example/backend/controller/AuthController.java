package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.exception.UserAlreadyExistsException;
import com.example.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        authService.registerUser(request.getEmail(), request.getPassword(), request.getUserName());
        return ResponseEntity.status(HttpStatus.CREATED).body("Registered Successfully");
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Map<String, String> token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleLoginFail() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleRegisterFail() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
    }
}
