package com.example.demo.controller;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RefreshRequestDto;
import com.example.demo.dto.RegisterRequestDto;
import com.example.demo.exception.TooManyRequestsException;
import com.example.demo.security.LoginRateLimiter;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequestDto dto) {
        authService.register(dto);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody LoginRequestDto dto,
                                 HttpServletRequest request) {
        String clientKey = request.getRemoteAddr();
        if (!loginRateLimiter.allow(clientKey)) {
            throw new TooManyRequestsException();
        }
        return authService.login(dto);
    }

    @PostMapping("/refresh")
    public AuthResponseDto refresh(@Valid @RequestBody RefreshRequestDto dto) {
        return authService.refresh(dto.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout(@Valid @RequestBody RefreshRequestDto dto) {
        authService.logout(dto.getRefreshToken());
    }
}