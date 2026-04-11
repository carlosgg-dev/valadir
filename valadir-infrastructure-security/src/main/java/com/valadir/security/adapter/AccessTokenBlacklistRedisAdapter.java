package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private final RedisTemplate<String, String> redisTemplate;

    public AccessTokenBlacklistRedisAdapter(final RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public void revoke(final String jti, final long remainingTtlSeconds) {

        try {
            redisTemplate.opsForValue().set(
                RedisKeySpace.forBlacklist(jti),
                RedisKeySpace.BLACKLIST_REVOKED_VALUE,
                Duration.ofSeconds(remainingTtlSeconds)
            );
        } catch (RedisConnectionFailureException e) {
            throw new InfrastructureException("Redis unavailable — token blacklist write failed for jti: " + jti, e);
        }
    }

    @Override
    public boolean isRevoked(final String jti) {

        try {
            return Objects.requireNonNullElse(redisTemplate.hasKey(RedisKeySpace.forBlacklist(jti)), false);
        } catch (RedisConnectionFailureException e) {
            throw new InfrastructureException("Redis unavailable — token blacklist read failed for jti: " + jti, e);
        }
    }
}
