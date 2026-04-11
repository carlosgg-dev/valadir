package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterAdapterExceptionTest {

    // Returns RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> failingTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    @Test
    void consume_redisUnavailable_throwsInfrastructureException() {

        final var adapter = new RedisRateLimiterAdapter(failingTemplate());

        assertThatThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, 60))
            .isInstanceOf(InfrastructureException.class);
    }
}
