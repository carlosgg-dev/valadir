package com.valadir.application.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLockedExceptionTest {

    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    @Test
    void constructor_setsAccountTemporarilyLockedCode() {

        var exception = new AccountLockedException(LOCKOUT);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
    }

    @Test
    void constructor_preservesLockoutDuration() {

        var exception = new AccountLockedException(LOCKOUT);

        assertThat(exception.lockout()).isEqualTo(LOCKOUT);
    }
}
