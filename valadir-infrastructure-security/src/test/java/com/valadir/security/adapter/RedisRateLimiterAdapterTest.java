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

        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void consume_firstRequest_isAllowed() {

        RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(1L);
        assertThat(result.maxRequests()).isEqualTo(MAX_REQUESTS);
        assertThat(result.remainingTtl()).isPositive();
    }

    @Test
    void consume_atLimit_isAllowed() {

        IntStream.range(0, MAX_REQUESTS - 1).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(MAX_REQUESTS);
    }

    @Test
    void consume_overLimit_isBlocked() {

        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isFalse();
        assertThat(result.requestCount()).isEqualTo(MAX_REQUESTS + 1);
        assertThat(result.remainingTtl()).isPositive(); // TTL must be returned even when blocked — used for Retry-After header
    }

    @Test
    void consume_ttlIsSet() {

        rateLimiter.consume(KEY, MAX_REQUESTS, 30);

        Long ttl = redisTemplate.getExpire(KEY);

        // Sliding window sets EXPIRE to window + 1 to keep entries alive for the full window duration
        assertThat(ttl)
            .isPositive()
            .isLessThanOrEqualTo(31L);
    }

    @Test
    void consume_ttlRefreshedOnEachRequest() {

        rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);
        // Simulates 50 seconds have passed → TTL = 10 seconds
        redisTemplate.expire(KEY, 10, TimeUnit.SECONDS);

        rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);
        Long ttl = redisTemplate.getExpire(KEY);

        // Sliding window always refreshes TTL to keep the sorted set alive while requests keep coming
        assertThat(ttl).isGreaterThan(10L);
    }

    @Test
    void consume_slidingWindow_requestsOutsideWindowDoNotCount() {

        long pastTimestamp = System.currentTimeMillis() - (WINDOW + 10) * 1000L;
        IntStream.rangeClosed(1, MAX_REQUESTS).forEach(i -> redisTemplate.opsForZSet().add(KEY, String.valueOf(i), pastTimestamp));
        redisTemplate.opsForValue().set(KEY + ":seq", String.valueOf(MAX_REQUESTS));

        RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        // Old entries are evicted by ZREMRANGEBYSCORE — only the current request counts
        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(1L);
    }

    @Test
    void consume_differentKeys_areIndependent() {

        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));

        RateLimitResult result = rateLimiter.consume("test:rate_limit:other_key", MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        assertThat(result.requestCount()).isEqualTo(1L);
    }

    @Test
    void consume_windowExpiry_resetsCounter() {

        // Reach the limit
        IntStream.range(0, MAX_REQUESTS).forEach(i -> rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW));
        assertThat(rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW).allowed()).isFalse();

        redisTemplate.delete(KEY);
        redisTemplate.delete(KEY + ":seq");

        RateLimitResult result = rateLimiter.consume(KEY, MAX_REQUESTS, WINDOW);

        assertThat(result.allowed()).isTrue();
        // Creates the key from 0
        assertThat(result.requestCount()).isEqualTo(1L);
    }
}
