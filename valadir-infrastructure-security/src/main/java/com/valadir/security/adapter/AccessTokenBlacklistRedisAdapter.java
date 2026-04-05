package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String REVOKED_VALUE = "revoked";

    private final RedisTemplate<String, String> redisTemplate;

    public AccessTokenBlacklistRedisAdapter(final RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public void revoke(final String jti, final long remainingTtlSeconds) {

        redisTemplate.opsForValue().set(blacklistKey(jti), REVOKED_VALUE, Duration.ofSeconds(remainingTtlSeconds));
    }

    @Override
    public boolean isRevoked(final String jti) {

        return Objects.requireNonNullElse(redisTemplate.hasKey(blacklistKey(jti)), false);
    }

    private String blacklistKey(final String jti) {

        return BLACKLIST_KEY_PREFIX + jti;
    }
}
