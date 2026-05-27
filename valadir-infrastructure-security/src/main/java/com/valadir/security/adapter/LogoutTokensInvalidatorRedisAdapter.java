package com.valadir.security.adapter;

import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

public class LogoutTokensInvalidatorRedisAdapter implements LogoutTokensInvalidator {

    private final RedisOperations<String, String> redisOperations;
    private final RedisScript<Long> logoutInvalidateTokensScript;

    public LogoutTokensInvalidatorRedisAdapter(RedisOperations<String, String> redisOperations) {

        this.redisOperations = redisOperations;
        this.logoutInvalidateTokensScript = RedisScript.of(new ClassPathResource("scripts/logout_invalidate_tokens.lua"), Long.class);
    }

    // Atomic: blacklists the access token and removes the refresh token from the user token set
    @Override
    public void invalidate(String jti, Duration remainingTtl, String refreshToken, AccountId accountId) {

        try {
            redisOperations.execute(
                logoutInvalidateTokensScript,
                List.of(
                    RedisKeySpace.forBlacklist(jti),
                    RedisKeySpace.forRefreshToken(refreshToken),
                    RedisKeySpace.forUserTokens(accountId.value().toString())
                ),
                RedisKeySpace.BLACKLIST_REVOKED_VALUE,
                String.valueOf(remainingTtl.getSeconds()),
                refreshToken
            );
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — logout token invalidation failed for jti: " + jti, e);
        }
    }
}
