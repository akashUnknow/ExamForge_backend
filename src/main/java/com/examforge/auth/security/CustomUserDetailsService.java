package com.examforge.auth.security;

import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Used only during login (via AuthenticationManager / DaoAuthenticationProvider).
 * All subsequent requests are authenticated statelessly from the JWT itself -
 * see JwtAuthenticationFilter - so this is not on the hot path for every request.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for the given email"));
        return UserPrincipal.fromUser(user);
    }
}
