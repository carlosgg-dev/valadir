package com.valadir.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notifications.async")
public record AsyncProperties(
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity) {

    public AsyncProperties {

        if (corePoolSize < 1) {
            throw new IllegalArgumentException("corePoolSize must be at least 1");
        }

        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maxPoolSize must be greater than or equal to corePoolSize");
        }

        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must not be negative");
        }
    }
}
