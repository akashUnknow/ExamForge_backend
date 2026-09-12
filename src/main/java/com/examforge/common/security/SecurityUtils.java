package com.examforge.common.security;

import com.examforge.auth.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentPrincipal().map(UserPrincipal::getId);
    }

    /**
     * The identifier stored in created_by/updated_by audit columns
     * (see JpaAuditingConfig) - the user's email.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentPrincipal().map(UserPrincipal::getUsername);
    }

    public static boolean currentUserHasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        var authorities = authentication.getAuthorities();
        for (String role : roles) {
            String target = "ROLE_" + role;
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals(target))) {
                return true;
            }
        }
        return false;
    }
}
