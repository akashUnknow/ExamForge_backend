package com.examforge.user.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.role.domain.Role;
import com.examforge.role.domain.RoleName;
import com.examforge.role.repository.RoleRepository;
import com.examforge.user.domain.User;
import com.examforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(String name, String email, String mobile, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedMobile = mobile.trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByMobile(normalizedMobile)) {
            throw new DuplicateResourceException("An account with this mobile number already exists");
        }

        Role defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default USER role is not configured"));

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setMobile(normalizedMobile);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setEmailVerified(false);
        user.addRole(defaultRole);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
