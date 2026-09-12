package com.examforge.auth.security;

import com.examforge.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Security principal wrapping our domain User.
 *
 * Two construction paths:
 *  - {@link #fromUser(User)} - used at login time, backed by a real DB entity
 *  - {@link #fromJwtClaims} - used by the JWT filter on every subsequent
 *    request, built entirely from token claims with no DB round trip
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(UUID id, String email, String passwordHash, boolean active, List<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = authorities;
    }

    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.isActive(), authorities);
    }

    public static UserPrincipal fromJwtClaims(UUID id, String email, Set<String> roles) {
        List<GrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        // Token possession already implies the account was active and
        // credentialed at issuance time; password hash is irrelevant here.
        return new UserPrincipal(id, email, null, true, authorities);
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
