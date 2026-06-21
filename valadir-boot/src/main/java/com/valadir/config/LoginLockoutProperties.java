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

        thresholds = List.copyOf(thresholds);
    }

    public record ThresholdProperties(
        int minFailures,
        Duration lockout) {

    }
}
