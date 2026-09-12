package com.examforge.common.validation;

/**
 * Implemented by any request DTO that carries a password + confirmation
 * pair, so {@link PasswordMatches} can validate them generically.
 */
public interface PasswordConfirmable {

    String getPassword();

    String getConfirmPassword();
}
