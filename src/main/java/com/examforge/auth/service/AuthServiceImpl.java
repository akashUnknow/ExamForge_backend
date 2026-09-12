package com.examforge.auth.service;

import com.examforge.auth.dto.AuthTokensResponse;
import com.examforge.auth.dto.LoginRequest;
import com.examforge.auth.dto.RegisterRequest;
import com.examforge.auth.security.UserPrincipal;
import com.examforge.common.exception.InvalidTokenException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.common.security.GeneratedToken;
import com.examforge.common.security.JwtProperties;
import com.examforge.common.security.JwtTokenProvider;
import com.examforge.common.security.TokenType;
import com.examforge.user.domain.User;
import com.examforge.user.dto.UserResponse;
import com.examforge.user.repository.UserRepository;
import com.examforge.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.examforge.audit");

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.getName(), request.getEmail(), request.getMobile(), request.getPassword());

        AUDIT_LOG.info("action=REGISTER userId={} email={}", user.getId(), user.getEmail());
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokensResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AUDIT_LOG.info("action=LOGIN userId={} email={}", user.getId(), user.getEmail());
        return issueTokenPair(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokensResponse refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.parseAndValidate(refreshToken, TokenType.REFRESH);
        UUID userId = jwtTokenProvider.getUserId(claims);
        String jti = jwtTokenProvider.getJti(claims);

        // Rotation: this refresh token can only be exchanged once.
        refreshTokenService.consumeForRotation(jti, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User for this token no longer exists"));

        if (!user.isActive()) {
            throw new InvalidTokenException("This account is no longer active");
        }

        AUDIT_LOG.info("action=TOKEN_REFRESH userId={}", user.getId());
        return issueTokenPair(user);
    }

    @Override
    public void logout(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseAndValidate(refreshToken, TokenType.REFRESH);
            String jti = jwtTokenProvider.getJti(claims);
            refreshTokenService.revoke(jti);
            AUDIT_LOG.info("action=LOGOUT userId={}", jwtTokenProvider.getUserId(claims));
        } catch (InvalidTokenException ex) {
            // Logging out with an already-invalid/expired token is a no-op,
            // not an error - the end state the caller wants is already true.
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    private AuthTokensResponse issueTokenPair(User user) {
        GeneratedToken access = jwtTokenProvider.generateAccessToken(user);
        GeneratedToken refresh = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenService.issue(refresh.jti(), user.getId(),
                Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMs()));

        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000;
        return AuthTokensResponse.of(access.token(), refresh.token(), expiresInSeconds, UserResponse.from(user));
    }
}
