package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record FullName(String value) {

    public FullName {

        if (value == null || value.isBlank()) {
            throw new DomainException("Full name is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        if (value.length() < 2) {
            throw new DomainException("Invalid full name", ErrorCode.INVALID_FIELD);
        }

        if (value.length() > 255) {
            throw new DomainException("Full name must not exceed 255 characters", ErrorCode.INVALID_FIELD);
        }
    }
}
