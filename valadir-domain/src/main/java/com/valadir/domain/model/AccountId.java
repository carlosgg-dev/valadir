package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {

        if (value == null) {
            throw new DomainException("Account ID cannot be null", ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }

    public static AccountId generate() {

        return new AccountId(UUID.randomUUID());
    }

    public static AccountId from(UUID value) {

        return new AccountId(value);
    }
}
