package com.examforge.auth.service;

import com.examforge.auth.dto.AuthTokensResponse;
import com.examforge.auth.dto.LoginRequest;
import com.examforge.auth.dto.RegisterRequest;
import com.examforge.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthTokensResponse login(LoginRequest request);

    AuthTokensResponse refresh(String refreshToken);

    void logout(String refreshToken);

    UserResponse getCurrentUser(java.util.UUID userId);
}
