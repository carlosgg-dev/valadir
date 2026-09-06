package com.valadir.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notifications.mail")
public record NotificationsProperties(
    String from) {

    public NotificationsProperties {

        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("notifications.mail.from must not be blank");
        }
    }
}
