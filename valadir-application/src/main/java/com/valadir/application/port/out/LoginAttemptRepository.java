package com.valadir.application.port.out;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginAttemptDecision;

import java.time.Duration;
import java.util.Optional;

public interface LoginAttemptRepository {

    LoginAttemptDecision decisionFor(Email email);

    Optional<Duration> recordFailedAttempt(Email email);

    void clearAttempts(Email email);
}
