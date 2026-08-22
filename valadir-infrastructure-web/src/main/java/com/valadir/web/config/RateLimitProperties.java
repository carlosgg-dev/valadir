package com.valadir.web.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties("rate-limit")
public record RateLimitProperties(
    boolean enabled,
    List<@Valid Rule> rules) {

    public RateLimitProperties {

        rules = rules == null
            ? List.of()
            : List.copyOf(rules);

        if (enabled && rules.isEmpty()) {
            throw new IllegalArgumentException("rate-limit.rules is required while rate-limit.enabled is true");
        }
    }

    public record Rule(
        @NotBlank String path,
        @NotNull Strategy strategy,
        @Positive int maxRequests,
        @NotNull Duration window) {

    }

    public enum Strategy {
        IP,
        EMAIL,
        USER
    }
}
