package com.examforge.auth.service;

import com.examforge.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Tracks the single set of "currently valid" refresh token IDs (jti) in
 * Redis, keyed by jti -> owning user id, with a TTL matching the token's
 * own expiration. This is what makes refresh-token rotation and logout
 * revocation possible for otherwise-stateless JWTs:
 *
 *  - issue(): called right after a refresh token is minted
 *  - consumeForRotation(): validates a jti belongs to the given user, then
 *    atomically removes it (rotation - a refresh token can only be used once)
 *  - revoke(): removes a jti outright (logout)
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    public void issue(String jti, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(key(jti), userId.toString(), ttl);
    }

    /**
     * Validates that the given jti is a currently-active refresh token
     * belonging to userId, then removes it (single-use rotation).
     * Throws InvalidTokenException if the token is unknown, expired, or
     * belongs to a different user.
     */
    public void consumeForRotation(String jti, UUID userId) {
        String key = key(jti);
        String owningUserId = redisTemplate.opsForValue().get(key);

        if (owningUserId == null) {
            throw new InvalidTokenException("Refresh token is invalid, expired, or has already been used");
        }
        if (!owningUserId.equals(userId.toString())) {
            throw new InvalidTokenException("Refresh token does not belong to this user");
        }

        redisTemplate.delete(key);
    }

    public void revoke(String jti) {
        redisTemplate.delete(key(jti));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
