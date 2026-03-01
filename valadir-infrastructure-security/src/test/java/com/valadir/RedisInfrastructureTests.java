package com.valadir;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisInfrastructureTests {

    private static final String REDIS_PASSWORD = "test_redis_pass";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379)
        .withCommand("redis-server --requirepass %s".formatted(REDIS_PASSWORD));

    @Test
    void should_fail_when_pinging_without_password() throws Exception {

        ExecResult result = redis.execInContainer("redis-cli", "ping");

        assertThat(result.getStdout())
            .as("Ping without password should return NOAUTH error")
            .contains("NOAUTH");
    }

    @Test
    void should_succeed_when_pinging_with_password() throws Exception {

        ExecResult result = redis.execInContainer("redis-cli", "-a", REDIS_PASSWORD, "ping");

        assertThat(result.getExitCode())
            .as("Ping with password should succeed")
            .isZero();

        assertThat(result.getStdout())
            .as("Redis should reply with PONG")
            .contains("PONG");
    }
}
