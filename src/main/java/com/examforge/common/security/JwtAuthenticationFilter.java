package com.examforge.common.security;

import com.examforge.auth.security.UserPrincipal;
import com.examforge.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs once per request. If a valid Bearer access token is present,
 * populates the SecurityContext from the token's claims alone - no
 * database lookup on the request hot path.
 *
 * If the header is absent, the request simply proceeds unauthenticated
 * (public endpoints allow it through; protected ones get rejected downstream
 * by Spring Security's access-decision logic). If the header is present but
 * invalid, the request also proceeds unauthenticated rather than failing
 * the filter outright, so the standard 401 JSON response is produced
 * consistently by the configured AuthenticationEntryPoint.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(token, TokenType.ACCESS);
                List<String> roles = jwtTokenProvider.getRoles(claims);
                Set<String> roleSet = roles == null ? new HashSet<>() : new HashSet<>(roles);

                UserPrincipal principal = UserPrincipal.fromJwtClaims(
                        jwtTokenProvider.getUserId(claims),
                        jwtTokenProvider.getEmail(claims),
                        roleSet
                );

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException ex) {
                log.debug("Rejected invalid access token: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
