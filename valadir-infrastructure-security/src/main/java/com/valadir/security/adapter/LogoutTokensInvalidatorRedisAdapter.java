package com.valadir.security.adapter;

import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

public class LogoutTokensInvalidatorRedisAdapter implements LogoutTokensInvalidator {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final RedisScript<Long> logoutInvalidateTokensScript;

    public LogoutTokensInvalidatorRedisAdapter(RedisOperations<String, String> redisOperations, RedisCircuitGuard circuitGuard) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.logoutInvalidateTokensScript = RedisScript.of(new ClassPathResource("scripts/logout_invalidate_tokens.lua"), Long.class);
    }

    // Atomic: blacklists the access token and removes the refresh token from the user token set,
    // only when that token belongs to the account logging out
    @Override
    public void invalidate(String jti, Duration remainingTtl, String refreshToken, AccountId accountId) {

        String accountIdValue = accountId.value().toString();

        circuitGuard.run("logout token invalidation failed for jti: " + jti, () ->
            redisOperations.execute(
                logoutInvalidateTokensScript,
                List.of(
                    RedisKeySpace.forBlacklist(jti),
                    RedisKeySpace.forRefreshToken(refreshToken),
                    RedisKeySpace.forUserTokens(accountIdValue)
                ),
                RedisKeySpace.BLACKLIST_REVOKED_VALUE,
                String.valueOf(remainingTtl.getSeconds()),
                refreshToken,
                accountIdValue
            )
        );
    }
}
