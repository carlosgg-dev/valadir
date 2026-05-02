package com.valadir.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("rate-limit")
public record RateLimitProperties(
    boolean enabled,
    List<Rule> rules) {

    public RateLimitProperties {

        rules = rules != null
            ? List.copyOf(rules)
            : List.of();
    }

    public record Rule(
        String path,
        Strategy strategy,
        int maxRequests,
        Duration window) {

    }

    public enum Strategy {
        IP,
        EMAIL,
        USER
    }
}
