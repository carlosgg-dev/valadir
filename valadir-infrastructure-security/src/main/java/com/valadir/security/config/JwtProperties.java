package com.valadir.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
    String privateKey,
    Duration accessTokenTtl,
    Duration refreshTokenTtl) {

}
