package com.examforge.common.security;

import com.examforge.common.exception.InvalidTokenException;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.user.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-must-be-at-least-256-bits-long!!");
        properties.setAccessTokenExpirationMs(60_000);
        properties.setRefreshTokenExpirationMs(120_000);

        tokenProvider = new JwtTokenProvider(properties);
        tokenProvider.init();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test.user@example.com");
        Role role = new Role(RoleName.USER, "Standard user");
        user.setRoles(Set.of(role));
    }

    @Test
    void generatedAccessToken_isValidAsAccessTokenOnly() {
        GeneratedToken token = tokenProvider.generateAccessToken(user);

        Claims claims = tokenProvider.parseAndValidate(token.token(), TokenType.ACCESS);
        assertThat(tokenProvider.getUserId(claims)).isEqualTo(user.getId());
        assertThat(tokenProvider.getEmail(claims)).isEqualTo(user.getEmail());
        assertThat(tokenProvider.getRoles(claims)).containsExactly("USER");

        assertThatThrownBy(() -> tokenProvider.parseAndValidate(token.token(), TokenType.REFRESH))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void generatedRefreshToken_isValidAsRefreshTokenOnly() {
        GeneratedToken token = tokenProvider.generateRefreshToken(user);

        Claims claims = tokenProvider.parseAndValidate(token.token(), TokenType.REFRESH);
        assertThat(tokenProvider.getJti(claims)).isEqualTo(token.jti());

        assertThatThrownBy(() -> tokenProvider.parseAndValidate(token.token(), TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedToken_isRejected() {
        GeneratedToken token = tokenProvider.generateAccessToken(user);
        String tampered = token.token().substring(0, token.token().length() - 2) + "xx";

        assertThatThrownBy(() -> tokenProvider.parseAndValidate(tampered, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void malformedToken_isRejected() {
        assertThatThrownBy(() -> tokenProvider.parseAndValidate("not-a-jwt", TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }
}
