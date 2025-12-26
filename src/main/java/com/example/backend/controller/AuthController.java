package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.AuthService;
import com.example.backend.service.JwtService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody RegisterRequest request) {
        UserDto userDto = authService.registerUser(request.getEmail(), request.getPassword(), request.getUserName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @PostMapping("/login")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login Successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "System error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(tokenResponse);
    }

    @PostMapping("/refresh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "re-new refresh token",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh Token is invalid or revoked",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        Map<String, String> newCoupleToken = jwtService.getAccessTokenFromRefreshToken(refreshToken);
        return ResponseEntity.status(HttpStatus.OK).body(newCoupleToken);
    }

    @GetMapping("/logout")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout sucessfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token not found or user hasn't login",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<?> logout() {
        authService.logout();
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "User has been logged out"));
    }
}
