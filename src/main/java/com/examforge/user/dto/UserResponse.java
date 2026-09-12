package com.examforge.user.dto;

import com.examforge.user.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public-facing representation of a user. Never includes the password hash.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String mobile,
        boolean active,
        boolean emailVerified,
        Set<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.isActive(),
                user.isEmailVerified(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
