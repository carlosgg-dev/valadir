package com.valadir.resilience.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

/**
 * A Redis container owned by the resilience suite, separate from the shared one in
 * {@code valadir-test-support}.
 *
 * <p>It exists to be paused. The shared container is a JVM-wide static reached by every other IT in
 * the fork, so pausing it would poison them all — an outage is therefore only ever injected from
 * this suite. Nothing outside this package may reuse this one.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IsolatedRedisContainerConfig {

    private static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));

    static {
        REDIS.start();
    }

    public static RedisContainer container() {

        return REDIS;
    }

    @Bean
    @ServiceConnection
    RedisContainer isolatedRedisContainer() {

        return REDIS;
    }
}
