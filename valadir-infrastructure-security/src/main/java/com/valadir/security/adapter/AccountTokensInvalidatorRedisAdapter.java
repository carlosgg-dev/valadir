package com.valadir.security.adapter;

import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.List;

public class AccountTokensInvalidatorRedisAdapter implements AccountTokensInvalidator {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final JwtProperties jwtProperties;
    private final RedisScript<Long> invalidateAccountTokensScript;

    public AccountTokensInvalidatorRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard,
        JwtProperties jwtProperties
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.jwtProperties = jwtProperties;
        this.invalidateAccountTokensScript = RedisScript.of(new ClassPathResource("scripts/invalidate_account_tokens.lua"), Long.class);
    }

    // Atomic: revokes every refresh token of the account, empties its token set, and cuts off the
    // access tokens already issued. Both halves of "this session is over" land together or not at all.
    @Override
    public void invalidateAll(AccountId accountId) {

        String accountIdValue = accountId.value().toString();

        circuitGuard.run("account token invalidation failed", () ->
            redisOperations.execute(
                invalidateAccountTokensScript,
                List.of(RedisKeySpace.forUserTokens(accountIdValue), RedisKeySpace.forTokenCutoff(accountIdValue)),
                RedisKeySpace.REFRESH_TOKEN_PREFIX,
                String.valueOf(Instant.now().getEpochSecond()),
                String.valueOf(jwtProperties.accessTokenTtl().getSeconds())
            )
        );
    }
}
