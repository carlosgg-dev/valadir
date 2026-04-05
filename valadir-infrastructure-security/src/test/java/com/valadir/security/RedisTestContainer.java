package com.valadir.security;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

public abstract class RedisTestContainer {

    protected static final RedisContainer REDIS;

    static {
        REDIS = createContainer();
        REDIS.start();
    }

    private static RedisContainer createContainer() {

        return new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");
    }
}
