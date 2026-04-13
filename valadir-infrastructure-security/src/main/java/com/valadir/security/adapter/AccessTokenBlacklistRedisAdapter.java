package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private final RedisTemplate<String, String> redisTemplate;

    public AccessTokenBlacklistRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isRevoked(String jti) {

        try {
            return Objects.requireNonNullElse(redisTemplate.hasKey(RedisKeySpace.forBlacklist(jti)), false);
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — token blacklist read failed for jti: " + jti, e);
        }
    }
}
