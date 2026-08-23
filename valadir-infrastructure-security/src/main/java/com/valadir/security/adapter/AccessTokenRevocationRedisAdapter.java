package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenRevocation;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.core.RedisOperations;

import java.time.Instant;
import java.util.List;

/**
 * Answers whether an access token is revoked, for the two reasons it can be: the token itself was
 * blacklisted (logout), or every token of its account issued up to a point in time was cut off
 * (password reset).
 */
public class AccessTokenRevocationRedisAdapter implements AccessTokenRevocation {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;

    public AccessTokenRevocationRedisAdapter(RedisOperations<String, String> redisOperations, RedisCircuitGuard circuitGuard) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
    }

    @Override
    public boolean isRevoked(String jti, String accountId, Instant issuedAt) {

        List<String> keys = List.of(RedisKeySpace.forBlacklist(jti), RedisKeySpace.forTokenCutoff(accountId));

        List<String> values = circuitGuard.call(
            "token revocation read failed for jti: " + jti,
            () -> redisOperations.opsForValue().multiGet(keys)
        );

        return values.getFirst() != null || issuedBeforeCutoff(issuedAt, values.getLast());
    }

    private static boolean issuedBeforeCutoff(Instant issuedAt, String cutoff) {

        return cutoff != null && issuedAt.getEpochSecond() <= Long.parseLong(cutoff);
    }
}
