package com.valadir.domain.policy;

import java.time.Duration;

public record LoginLockoutThreshold(
    int minFailures,
    Duration lockout) {

    public LoginLockoutThreshold {

        validatePositiveMinFailures(minFailures);
        validatePositiveLockout(lockout);
    }

    private static void validatePositiveMinFailures(int minFailures) {

        if (minFailures <= 0) {
            throw new IllegalArgumentException("Lockout threshold minFailures must be a positive number of failures");
        }
    }

    private static void validatePositiveLockout(Duration lockout) {

        if (lockout == null || !lockout.isPositive()) {
            throw new IllegalArgumentException("Lockout threshold duration must be a positive duration");
        }
    }
}
