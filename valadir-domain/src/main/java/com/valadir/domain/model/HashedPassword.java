package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record HashedPassword(String value) {

    public HashedPassword {

        if (value == null || value.isBlank()) {
            throw new DomainException("Hashed password cannot be empty", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
        }
    }
}
