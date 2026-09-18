package com.valadir.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("auth.lockout")
public record LoginLockoutProperties(
    Duration window,
    int challengeThreshold,
    List<ThresholdProperties> thresholds) {

    public LoginLockoutProperties {

        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("auth.lockout.thresholds must declare at least one threshold");
        }

        thresholds = List.copyOf(thresholds);
    }

    public record ThresholdProperties(
        int minFailures,
        Duration lockout) {

    }
}
