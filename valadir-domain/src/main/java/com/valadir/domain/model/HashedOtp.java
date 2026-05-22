package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record HashedOtp(String value) {

    public HashedOtp {

        if (value == null || value.isBlank()) {
            throw new DomainException("HashedOtp value must not be blank", ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }
}
