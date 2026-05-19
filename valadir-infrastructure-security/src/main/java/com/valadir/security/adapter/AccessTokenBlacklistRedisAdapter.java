package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private final RedisTemplate<String, String> redisTemplate;

    public AccessTokenBlacklistRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isRevoked(String jti) {

        try {
            return Objects.requireNonNullElse(redisTemplate.hasKey(RedisKeySpace.forBlacklist(jti)), false);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — token blacklist read failed for jti: " + jti, e);
        }
    }
}
