package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {

        if (value == null) {
            throw new DomainException("User ID cannot be null", ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }

    public static UserId generate() {

        return new UserId(UUID.randomUUID());
    }

    public static UserId from(UUID value) {

        return new UserId(value);
    }
}
