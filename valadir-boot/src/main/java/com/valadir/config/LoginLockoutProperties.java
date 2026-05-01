package com.valadir.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("auth.lockout")
public record LoginLockoutProperties(
    long windowSeconds,
    List<ThresholdProperties> thresholds) {

    public LoginLockoutProperties {

        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("auth.lockout.thresholds requires at least one threshold entry in application.yml");
        }

        thresholds = List.copyOf(thresholds);
    }

    public record ThresholdProperties(
        int minFailures,
        long lockoutSeconds) {

    }
}
