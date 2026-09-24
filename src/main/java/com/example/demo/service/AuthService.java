package com.example.demo.service;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RegisterRequestDto;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    @Transactional
    public void register(RegisterRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.STUDENT); // роль всегда назначает сервер, не клиент
        userRepository.save(user);
    }
    @Transactional
    public AuthResponseDto login(LoginRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }
    @Transactional
    public AuthResponseDto refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(TokenHashUtil.sha256(rawRefreshToken))
                .orElseThrow(InvalidTokenException::new);

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException();
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository
                .findByTokenHash(TokenHashUtil.sha256(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
    private AuthResponseDto issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String rawRefreshToken = jwtUtil.generateRefreshTokenRaw();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHashUtil.sha256(rawRefreshToken));
        refreshToken.setExpiresAt(LocalDateTime.now()
                .plus(Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMs())));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponseDto(accessToken, rawRefreshToken);
    }
}