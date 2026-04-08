package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.security.redis.RedisKeySpace;
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

        redisTemplate.opsForValue().set(
            RedisKeySpace.forBlacklist(jti),
            RedisKeySpace.BLACKLIST_REVOKED_VALUE,
            Duration.ofSeconds(remainingTtlSeconds)
        );
    }

    @Override
    public boolean isRevoked(final String jti) {

        return Objects.requireNonNullElse(redisTemplate.hasKey(RedisKeySpace.forBlacklist(jti)), false);
    }
}
