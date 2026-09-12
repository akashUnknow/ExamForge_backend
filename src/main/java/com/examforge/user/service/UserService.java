package com.examforge.user.service;

import com.examforge.user.domain.User;

public interface UserService {

    /**
     * Creates a new, active user with the default USER role.
     * Throws DuplicateResourceException if the email or mobile is already taken.
     */
    User createUser(String name, String email, String mobile, String rawPassword);

    User getByEmail(String email);
}
