package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDto {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}