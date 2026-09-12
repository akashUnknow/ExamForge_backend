package com.examforge.common.security;

import com.examforge.common.exception.InvalidTokenException;
import com.examforge.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates and validates access and refresh JWTs.
 *
 * Access tokens are fully stateless (no server-side lookup required).
 * Refresh tokens carry a jti that the caller (RefreshTokenService) tracks
 * in Redis, which is what makes rotation and revocation possible.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret is shorter than 256 bits - this is INSECURE outside local development. " +
                    "Set a strong JWT_SECRET before deploying.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public GeneratedToken generateAccessToken(User user) {
        return generateToken(user, TokenType.ACCESS, jwtProperties.getAccessTokenExpirationMs());
    }

    public GeneratedToken generateRefreshToken(User user) {
        return generateToken(user, TokenType.REFRESH, jwtProperties.getRefreshTokenExpirationMs());
    }

    private GeneratedToken generateToken(User user, TokenType type, long expirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        String jti = UUID.randomUUID().toString();

        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        String token = Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiry))
                .signWith(signingKey)
                .compact();

        return new GeneratedToken(token, jti, expiry);
    }

    /**
     * Parses and fully validates a token (signature + expiry), and asserts
     * it is of the expected type. Throws InvalidTokenException on any failure.
     */
    public Claims parseAndValidate(String token, TokenType expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token is invalid");
        }

        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (actualType == null || !actualType.equals(expectedType.name())) {
            throw new InvalidTokenException("Token type mismatch: expected " + expectedType);
        }

        return claims;
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        return claims.get(CLAIM_ROLES, List.class);
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }
}
