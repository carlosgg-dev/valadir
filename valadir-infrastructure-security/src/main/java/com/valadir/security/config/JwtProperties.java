package com.valadir.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String privateKey,
    long accessTokenTtlSeconds,
    long refreshTokenTtlSeconds) {

}
