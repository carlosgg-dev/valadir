package com.valadir.domain.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptDecisionTest {

    @Test
    void lockedOut_exposesRemainingDuration() {

        var remaining = Duration.ofMinutes(5);

        var decision = new LoginAttemptDecision.LockedOut(remaining);

        assertThat(decision.remaining()).isEqualTo(remaining);
    }
}
