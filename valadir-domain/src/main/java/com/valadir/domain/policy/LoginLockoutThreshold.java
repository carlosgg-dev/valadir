package com.valadir.domain.policy;

import java.time.Duration;

public record LoginLockoutThreshold(
    int minFailures,
    Duration lockout) {

}
