package com.valadir.security.adapter;

import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.TokenValidationResult;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.UUID;

public class RefreshTokenStoreRedisAdapter implements RefreshTokenStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final RedisScript<Long> saveRefreshTokenScript;
    private final RedisScript<Long> rotateRefreshTokenScript;
    private final RedisScript<Long> revokeAllRefreshTokensScript;

    public RefreshTokenStoreRedisAdapter(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {

        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.saveRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/save_refresh_token.lua"), Long.class);
        this.rotateRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/rotate_refresh_token.lua"), Long.class);
        this.revokeAllRefreshTokensScript = RedisScript.of(new ClassPathResource("scripts/revoke_all_refresh_tokens.lua"), Long.class);
    }

    @Override
    public TokenValidationResult validate(String token) {

        try {
            String accountIdValue = redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(token));
            return accountIdValue == null
                ? new TokenValidationResult.Invalid()
                : new TokenValidationResult.Valid(new AccountId(UUID.fromString(accountIdValue)));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — refresh token validation failed", e);
        }
    }

    // Atomic: stores the refresh token and registers it in the user's token set
    @Override
    public void save(String token, AccountId accountId) {

        try {
            String accountIdStr = accountId.value().toString();
            redisTemplate.execute(
                saveRefreshTokenScript,
                List.of(RedisKeySpace.forRefreshToken(token), RedisKeySpace.forUserTokens(accountIdStr)),
                accountIdStr,
                String.valueOf(jwtProperties.refreshTokenTtl().getSeconds()),
                token
            );
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — refresh token save failed", e);
        }
    }

    // Atomic: removes old token and stores new one with TTL. Returns false if old token no longer exists
    @Override
    public boolean rotate(String oldToken, String newToken, AccountId accountId) {

        try {
            String accountIdStr = accountId.value().toString();
            Long result = redisTemplate.execute(
                rotateRefreshTokenScript,
                List.of(
                    RedisKeySpace.forRefreshToken(oldToken),
                    RedisKeySpace.forRefreshToken(newToken),
                    RedisKeySpace.forUserTokens(accountIdStr)
                ),
                oldToken,
                newToken,
                accountIdStr,
                String.valueOf(jwtProperties.refreshTokenTtl().getSeconds())
            );
            return Long.valueOf(1L).equals(result);
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — refresh token rotation failed", e);
        }
    }

    // Atomic: revokes all refresh tokens for the account.
    // Active access tokens remain valid until they expire — intentional trade-off to avoid per-user JTI tracking.
    @Override
    public void revokeAllForAccount(AccountId accountId) {

        try {
            redisTemplate.execute(
                revokeAllRefreshTokensScript,
                List.of(RedisKeySpace.forUserTokens(accountId.value().toString())),
                RedisKeySpace.REFRESH_TOKEN_PREFIX
            );
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — refresh token revocation failed", e);
        }
    }
}
