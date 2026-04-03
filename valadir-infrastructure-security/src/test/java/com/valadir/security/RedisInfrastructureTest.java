package com.valadir.security;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisInfrastructureTest {

    private static final String REDIS_PASSWORD = "test_redis_pass";

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379)
        .withCommand("redis-server --requirepass %s".formatted(REDIS_PASSWORD));

    @Test
    void ping_withoutPassword_returnsNoauthError() throws Exception {

        ExecResult result = redis.execInContainer("redis-cli", "ping");

        assertThat(result.getStdout())
            .as("Ping without password should return NOAUTH error")
            .contains("NOAUTH");
    }

    @Test
    void ping_withValidPassword_returnsPong() throws Exception {

        ExecResult result = redis.execInContainer("redis-cli", "-a", REDIS_PASSWORD, "ping");

        assertThat(result.getExitCode())
            .as("Ping with password should succeed")
            .isZero();

        assertThat(result.getStdout())
            .as("Redis should reply with PONG")
            .contains("PONG");
    }
}
