package com.example.demo.security;

import com.example.demo.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET environment variable.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
    public String generateAccessToken(String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
    public String generateRefreshTokenRaw() {
        // случайная строка, не JWT — просто длинный случайный токен
        return java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
    }
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}