package com.valadir.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
    @NotBlank String privateKey,
    @NotNull Duration accessTokenTtl,
    @NotNull Duration refreshTokenTtl) {

}
