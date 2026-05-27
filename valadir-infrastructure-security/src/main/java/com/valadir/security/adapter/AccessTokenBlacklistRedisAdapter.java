package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Objects;

public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private final RedisOperations<String, String> redisOperations;

    public AccessTokenBlacklistRedisAdapter(RedisOperations<String, String> redisOperations) {

        this.redisOperations = redisOperations;
    }

    @Override
    public boolean isRevoked(String jti) {

        try {
            return Objects.requireNonNullElse(redisOperations.hasKey(RedisKeySpace.forBlacklist(jti)), false);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — token blacklist read failed for jti: " + jti, e);
        }
    }
}
