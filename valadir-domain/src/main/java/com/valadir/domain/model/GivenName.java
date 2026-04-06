package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record GivenName(String value) {

    public GivenName {

        if (value != null && value.isBlank()) {
            value = null;
        }

        if (value != null && value.length() > 100) {
            throw new DomainException("Given name must not exceed 100 characters", ErrorCode.INVALID_FIELD);
        }
    }

    public static GivenName empty() {

        return new GivenName(null);
    }
}
