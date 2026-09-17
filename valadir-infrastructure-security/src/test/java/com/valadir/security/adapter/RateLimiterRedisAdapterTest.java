package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.ratelimit.RateLimitStrategy;
import com.valadir.common.ratelimit.RateLimitSubject;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RateLimiterRedisAdapterTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final RateLimitSubject SUBJECT = new RateLimitSubject(RateLimitStrategy.IP, "/api/auth/login", "10.0.0.1");

    @Test
    void consume_redisError_throwsInfrastructureException() {

        var adapter = new RateLimiterRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.consume(SUBJECT, 10, WINDOW))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
