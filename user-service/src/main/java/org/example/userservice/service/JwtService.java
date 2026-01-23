package org.example.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.TokenResponse;
import org.example.userservice.exception.RefreshTokenRevokeException;
import org.example.userservice.model.RefreshToken;
import org.example.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${token.secret}")
    private String jwtSecretKey;

    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30;  // 1 hour
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 ngày
    private final RefreshTokenRepository refreshTokenRepository;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
    }

    public String generateAccessToken(String user_id) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id",  user_id);
        return createToken(claims, user_id, ACCESS_TOKEN_EXPIRATION);
    }
    public String generateRefreshToken(String user_id) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id",  user_id);
        String refreshToken = createToken(claims, user_id, REFRESH_TOKEN_EXPIRATION);

        refreshTokenRepository.save(RefreshToken.builder()
                .expires_at(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .isRevoked(false)
                .userId(user_id)
                .token(refreshToken)
                .build());

        return refreshToken;
    }

    private String createToken(Map<String, Object> claims, String userId, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public TokenResponse getAccessTokenFromRefreshToken(String refreshToken) throws RefreshTokenRevokeException {
        String userId = extractUserId(refreshToken);

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken);
        if(!token.getIsRevoked()) {
            final String newAccessToken = generateAccessToken(userId);
            final String newRefreshToken = generateRefreshToken(userId);

            token.setIsRevoked(true);
            refreshTokenRepository.save(token);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken(newAccessToken);
            tokenResponse.setRefreshToken(newRefreshToken);

            return tokenResponse;
        }
        else {
            throw new RefreshTokenRevokeException("refresh token is revoke");
        }
    }


    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String extractUserId(String token) {
        Object id = parseToken(token).get("user_id");
        if (id instanceof String i) return id.toString();
        return null;
    }

    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
    private boolean isTokenExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
