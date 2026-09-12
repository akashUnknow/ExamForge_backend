package com.examforge.auth.dto;

import com.examforge.user.dto.UserResponse;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
    public static AuthTokensResponse of(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
        return new AuthTokensResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
