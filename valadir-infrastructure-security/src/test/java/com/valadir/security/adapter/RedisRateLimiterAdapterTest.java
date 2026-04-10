package com.valadir.security.adapter;

import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.security.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisRateLimiterAdapterTest extends RedisTestContainer {

    private static final String KEY = "test:rate_limit:key";
    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW = 60;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {

        final RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (final var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void consume_firstRequest_isAllowed() {

        final RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(1L);
        assertThat(result.maxRequests()).isEqualTo(MAX_REQUESTS);
        assertThat(result.remainingTtl()).isPositive();
    }

    @Test
    void consume_atLimit_isAllowed() {

        IntStream.range(0, MAX_REQUESTS - 1).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        final RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(MAX_REQUESTS);
    }

    @Test
    void consume_overLimit_isBlocked() {

        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        final RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isFalse();
        assertThat(result.requestCount()).isEqualTo(MAX_REQUESTS + 1);
        assertThat(result.remainingTtl()).isPositive(); // TTL must be returned even when blocked — used for Retry-After header
    }

    @Test
    void consume_ttlIsSet() {

        rateLimiter.consume(KEY, MAX_REQUESTS, 30);

        final Long ttl = redisTemplate.getExpire(KEY);

        assertThat(ttl).isPositive().isLessThanOrEqualTo(30L);
    }

    @Test
    void consume_ttlNotResetOnSubsequentRequests() {

        rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);
        // Simulates 50 seconds have passed → TTL = 10 seconds
        redisTemplate.expire(KEY, 10, TimeUnit.SECONDS);

        rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);
        final Long ttl = redisTemplate.getExpire(KEY);

        // If EXPIRE ran again on the second call, TTL would be back to WINDOW (60s)
        assertThat(ttl).isLessThanOrEqualTo(10L);
    }

    @Test
    void consume_differentKeys_areIndependent() {

        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        final RateLimitResult result = rateLimiter.consume("test:rate_limit:other_key", MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(1L);
    }

    @Test
    void consume_windowExpiry_resetsCounter() {

        // Reach the limit
        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));
        assertThat(rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW).allowed()).isFalse();

        redisTemplate.delete(KEY); // simulate window expiry

        final RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        // Creates the key from 0
        assertThat(result.requestCount()).isEqualTo(1L);
    }
}
