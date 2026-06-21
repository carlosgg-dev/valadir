package com.valadir.domain.policy;

import java.time.Duration;

public sealed interface LoginAttemptDecision {

    record Allowed() implements LoginAttemptDecision {

    }

    record ChallengeRequired() implements LoginAttemptDecision {

    }

    record LockedOut(Duration remaining) implements LoginAttemptDecision {

    }
}
