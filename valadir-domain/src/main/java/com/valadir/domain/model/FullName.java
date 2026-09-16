package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record FullName(String value) {

    public FullName {

        value = value == null ? null : value.trim();

        if (value == null || value.isBlank()) {
            throw new DomainException("Full name is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new DomainException("Full name must not contain control characters", ErrorCode.INVALID_FIELD);
        }

        // A code point scan pairs the surrogates first, so only an unpaired one is left reading as SURROGATE; the driver
        // would encode it as '?' and store a name nobody typed
        if (value.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.SURROGATE)) {
            throw new DomainException("Full name must not contain unpaired surrogates", ErrorCode.INVALID_FIELD);
        }

        if (value.length() < 2) {
            throw new DomainException("Invalid full name", ErrorCode.INVALID_FIELD);
        }

        if (value.length() > 255) {
            throw new DomainException("Full name must not exceed 255 characters", ErrorCode.INVALID_FIELD);
        }
    }

    public static FullName from(String value) {

        return new FullName(value);
    }
}
