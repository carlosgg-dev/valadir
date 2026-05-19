package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class RedisRateLimiterAdapter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> rateLimitScript;

    public RedisRateLimiterAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(new ClassPathResource("scripts/rate_limit.lua"), List.class);
    }

    @Override
    public RateLimitResult consume(String key, int maxRequests, Duration window) {

        try {
            List<?> result = Objects.requireNonNull(
                redisTemplate.execute(rateLimitScript, List.of(key),
                                      String.valueOf(maxRequests),
                                      String.valueOf(window.getSeconds()),
                                      String.valueOf(System.currentTimeMillis())),
                "Rate limit script returned no result for key: " + key
            );

            long requestCount = (Long) result.get(0);
            Duration remainingTtl = Duration.ofSeconds((Long) result.get(1));

            return new RateLimitResult(requestCount <= maxRequests, requestCount, maxRequests, remainingTtl);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — rate limit check failed", e);
        }
    }
}
