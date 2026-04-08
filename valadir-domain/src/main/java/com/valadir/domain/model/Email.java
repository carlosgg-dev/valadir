package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record Email(String value) {

    public Email {

        if (value == null || value.isBlank()) {
            throw new DomainException("Email is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        if (value.length() > 255) {
            throw new DomainException("Email must not exceed 255 characters", ErrorCode.INVALID_FIELD);
        }

        int delimiterIndex = value.indexOf('@');

        if (delimiterIndex <= 0 || delimiterIndex != value.lastIndexOf('@')) {
            throw new DomainException("Invalid email format", ErrorCode.INVALID_FIELD);
        }

        String domain = value.substring(delimiterIndex + 1);

        if (!domain.contains(".")) {
            throw new DomainException("Invalid email format", ErrorCode.INVALID_FIELD);
        }
    }
}
