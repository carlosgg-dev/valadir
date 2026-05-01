package com.valadir.application.port.out;

import com.valadir.domain.model.Email;

import java.time.Duration;
import java.util.Optional;

public interface LoginAttemptStore {

    Optional<Duration> findActiveLockout(Email email);

    void recordFailedAttempt(Email email);

    void clearAttempts(Email email);
}
