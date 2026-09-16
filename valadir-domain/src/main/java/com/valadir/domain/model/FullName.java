package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record FullName(String value) {

    private static final String LABEL = "Full name";

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 255;

    public FullName {

        value = Names.normalize(value);
        requirePresent(value);
        Names.rejectHiddenCharacters(value, LABEL);
        requireLengthWithinBounds(value);
    }

    public static FullName from(String value) {

        return new FullName(value);
    }

    private static void requirePresent(String value) {

        if (value == null || value.isBlank()) {
            throw new DomainException(LABEL + " is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }

    private static void requireLengthWithinBounds(String value) {

        if (value.length() < MIN_LENGTH) {
            throw new DomainException("Invalid full name", ErrorCode.INVALID_FIELD);
        }

        if (value.length() > MAX_LENGTH) {
            throw new DomainException(LABEL + " must not exceed " + MAX_LENGTH + " characters", ErrorCode.INVALID_FIELD);
        }
    }
}
