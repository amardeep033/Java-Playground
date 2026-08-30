package com.example.jwtrbac.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
