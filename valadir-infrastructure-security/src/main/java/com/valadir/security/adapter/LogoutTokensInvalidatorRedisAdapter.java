package com.valadir.security.adapter;

import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class LogoutTokensInvalidatorRedisAdapter implements LogoutTokensInvalidator {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> logoutInvalidateTokensScript;

    public LogoutTokensInvalidatorRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
        this.logoutInvalidateTokensScript = RedisScript.of(new ClassPathResource("scripts/logout_invalidate_tokens.lua"), Long.class);
    }

    // Atomic: blacklists the access token and removes the refresh token from the user token set
    @Override
    public void invalidate(String jti, Duration remainingTtl, String refreshToken, String accountId) {

        try {
            redisTemplate.execute(
                logoutInvalidateTokensScript,
                List.of(
                    RedisKeySpace.forBlacklist(jti),
                    RedisKeySpace.forRefreshToken(refreshToken),
                    RedisKeySpace.forUserTokens(accountId)
                ),
                RedisKeySpace.BLACKLIST_REVOKED_VALUE,
                String.valueOf(remainingTtl.getSeconds()),
                refreshToken
            );
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — logout token invalidation failed for jti: " + jti, e);
        }
    }
}
