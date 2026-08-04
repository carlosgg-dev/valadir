package com.valadir.security.adapter;

import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.security.redis.RedisCircuitGuard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class RateLimiterRedisAdapter implements RateLimiter {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final RedisScript<List> rateLimitScript;

    public RateLimiterRedisAdapter(RedisOperations<String, String> redisOperations, RedisCircuitGuard circuitGuard) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.rateLimitScript = RedisScript.of(new ClassPathResource("scripts/rate_limit.lua"), List.class);
    }

    @Override
    public RateLimitResult consume(String key, int maxRequests, Duration window) {

        List<?> result = circuitGuard.call("rate limit check failed", () -> Objects.requireNonNull(
            redisOperations.execute(rateLimitScript, List.of(key),
                                    String.valueOf(maxRequests),
                                    String.valueOf(window.getSeconds()),
                                    String.valueOf(System.currentTimeMillis())),
            "Rate limit script returned no result for key: " + key
        ));

        long requestCount = (Long) result.get(0);
        Duration remainingTtl = Duration.ofSeconds((Long) result.get(1));

        return new RateLimitResult(requestCount <= maxRequests, requestCount, maxRequests, remainingTtl);
    }
}
