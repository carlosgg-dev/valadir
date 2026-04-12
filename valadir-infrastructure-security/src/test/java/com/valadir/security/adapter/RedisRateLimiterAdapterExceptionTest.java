package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterAdapterExceptionTest {

    // Throws RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> connectionFailureTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    // Throws RedisSystemException on any call — simulates command-level Redis errors (e.g. script execution failure)
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> systemErrorTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisSystemException("ERR command not allowed", null);
        });
    }

    @Test
    void consume_redisConnectionFailure_throwsInfrastructureException() {

        final var adapter = new RedisRateLimiterAdapter(connectionFailureTemplate());

        assertThatThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, 60))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void consume_redisSystemError_throwsInfrastructureException() {

        final var adapter = new RedisRateLimiterAdapter(systemErrorTemplate());

        assertThatThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, 60))
            .isInstanceOf(InfrastructureException.class);
    }
}
