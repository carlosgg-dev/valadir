package com.valadir.domain.exception;

import com.valadir.common.error.ErrorCode;

public class AccountLockedException extends DomainException {

    private final long retryAfterSeconds;

    public AccountLockedException(long retryAfterSeconds) {

        super("Account temporarily locked", ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {

        return retryAfterSeconds;
    }
}
