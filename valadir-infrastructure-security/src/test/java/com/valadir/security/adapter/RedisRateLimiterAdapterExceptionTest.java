package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterAdapterExceptionTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private RedisRateLimiterAdapter adapter;

    @Test
    void consume_redisError_throwsInfrastructureException() {

        given(redisTemplate.execute(any(), anyList(), any(), any(), any())).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.consume("rate_limit:ip:test", 10, WINDOW))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE)
            .withCause(REDIS_ERROR);
    }
}
