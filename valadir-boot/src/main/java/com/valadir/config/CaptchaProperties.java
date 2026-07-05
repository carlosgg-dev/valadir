package com.valadir.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.captcha")
public record CaptchaProperties(
    String verifyUrl,
    String secret,
    Duration connectTimeout,
    Duration readTimeout,
    boolean enabled) {

}
