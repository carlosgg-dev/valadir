package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.regex.Pattern;

public record RawPassword(String value) {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private static final Pattern HAS_NUMBER = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[@#$%^&+=!_\\-].*");

    public RawPassword {

        if (value == null || value.isBlank()) {
            throw new DomainException("Password cannot be empty", ErrorCode.INVALID_PASSWORD);
        }

        if (value.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainException("Password is too long", ErrorCode.INVALID_PASSWORD);
        }

        if (value.length() < MIN_PASSWORD_LENGTH ||
            !HAS_NUMBER.matcher(value).matches() ||
            !HAS_LOWER.matcher(value).matches() ||
            !HAS_UPPER.matcher(value).matches() ||
            !HAS_SPECIAL.matcher(value).matches()) {

            throw new DomainException(
                "The password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.",
                ErrorCode.INVALID_PASSWORD
            );
        }
    }
}
