package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record GivenName(String value) {

    public GivenName {

        value = value == null ? null : value.trim();

        if (value != null && value.isBlank()) {
            value = null;
        }

        if (value != null && value.chars().anyMatch(Character::isISOControl)) {
            throw new DomainException("Given name must not contain control characters", ErrorCode.INVALID_FIELD);
        }

        if (value != null && value.length() > 100) {
            throw new DomainException("Given name must not exceed 100 characters", ErrorCode.INVALID_FIELD);
        }
    }

    public static GivenName from(String value) {

        return new GivenName(value);
    }
}
