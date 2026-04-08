package com.valadir.security.adapter;

import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RefreshTokenRedisAdapter implements RefreshTokenStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final RedisScript<Long> saveRefreshTokenScript;
    private final RedisScript<Long> rotateRefreshTokenScript;

    public RefreshTokenRedisAdapter(final RedisTemplate<String, String> redisTemplate, final JwtProperties jwtProperties) {

        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.saveRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/save_refresh_token.lua"), Long.class);
        this.rotateRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/rotate_refresh_token.lua"), Long.class);
    }

    @Override
    public TokenValidationResult validate(final String token) {

        final String accountIdValue = redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(token));

        return accountIdValue == null
            ? new TokenValidationResult.Invalid()
            : new TokenValidationResult.Valid(new AccountId(UUID.fromString(accountIdValue)));
    }

    // Atomic: stores the refresh token and registers it in the user's token set
    @Override
    public void save(final String token, final AccountId accountId) {

        redisTemplate.execute(
            saveRefreshTokenScript,
            List.of(RedisKeySpace.forRefreshToken(token)),
            accountId.value().toString(),
            String.valueOf(jwtProperties.refreshTokenTtlSeconds()),
            token
        );
    }

    // Atomic: removes old token and stores new one with TTL. Returns false if old token no longer exists
    @Override
    public boolean rotate(final String oldToken, final String newToken, final AccountId accountId) {

        final Long result = redisTemplate.execute(
            rotateRefreshTokenScript,
            List.of(RedisKeySpace.forRefreshToken(oldToken), RedisKeySpace.forRefreshToken(newToken)),
            oldToken,
            newToken,
            accountId.value().toString(),
            String.valueOf(jwtProperties.refreshTokenTtlSeconds())
        );

        return Long.valueOf(1L).equals(result);
    }
}
