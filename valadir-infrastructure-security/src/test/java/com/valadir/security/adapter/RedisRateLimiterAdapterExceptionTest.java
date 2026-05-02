package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterAdapterExceptionTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);

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

        var adapter = new RedisRateLimiterAdapter(connectionFailureTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, WINDOW))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void consume_redisSystemError_throwsInfrastructureException() {

        var adapter = new RedisRateLimiterAdapter(systemErrorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, WINDOW))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }
}
