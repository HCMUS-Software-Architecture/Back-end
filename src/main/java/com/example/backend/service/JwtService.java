package com.example.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
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
        return createToken(claims, null, REFRESH_TOKEN_EXPIRATION);
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

    public String getAccessTokenFromRefreshToken(String refreshToken) {
        String user_name = extractUsername(refreshToken);
        String id = extractUserId(refreshToken);
        return generateAccessToken(id);
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
