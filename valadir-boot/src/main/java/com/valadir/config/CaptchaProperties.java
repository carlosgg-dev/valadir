package com.valadir.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("auth.captcha")
public record CaptchaProperties(
    String verifyUrl,
    String secret,
    // Bound unconditionally, unlike the endpoint and the secret above: captchaRestClient builds the
    // client from both whether or not the captcha is enabled.
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    boolean enabled) {

    public CaptchaProperties {

        if (enabled && (verifyUrl == null || verifyUrl.isBlank() || secret == null || secret.isBlank())) {
            throw new IllegalArgumentException(
                "auth.captcha.verify-url and auth.captcha.secret are required while auth.captcha.enabled is true");
        }
    }
}
