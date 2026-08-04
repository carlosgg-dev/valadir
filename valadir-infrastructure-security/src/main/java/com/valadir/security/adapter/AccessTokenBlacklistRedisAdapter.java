package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.core.RedisOperations;

import java.util.Objects;

public class AccessTokenBlacklistRedisAdapter implements AccessTokenBlacklist {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;

    public AccessTokenBlacklistRedisAdapter(RedisOperations<String, String> redisOperations, RedisCircuitGuard circuitGuard) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
    }

    @Override
    public boolean isRevoked(String jti) {

        String redisKey = RedisKeySpace.forBlacklist(jti);

        return circuitGuard.call(
            "token blacklist read failed for jti: " + jti,
            () -> Objects.requireNonNullElse(redisOperations.hasKey(redisKey), false)
        );
    }
}
