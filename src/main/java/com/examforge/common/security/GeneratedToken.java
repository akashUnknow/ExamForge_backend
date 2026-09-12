package com.examforge.common.security;

import java.time.Instant;

/**
 * Result of generating a JWT: the raw token string plus the metadata
 * (jti, expiry) the caller needs for refresh-token tracking in Redis.
 */
public record GeneratedToken(String token, String jti, Instant expiresAt) {
}
