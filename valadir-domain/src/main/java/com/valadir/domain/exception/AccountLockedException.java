package com.valadir.domain.exception;

import com.valadir.common.error.ErrorCode;

import java.time.Duration;

public class AccountLockedException extends DomainException {

    private final Duration lockout;

    public AccountLockedException(Duration lockout) {

        super("Account temporarily locked", ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
        this.lockout = lockout;
    }

    public Duration lockout() {

        return lockout;
    }
}
