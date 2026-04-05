package com.valadir.security.adapter;

import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RefreshTokenRedisAdapter implements RefreshTokenStore {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:";
    private static final String USER_TOKENS_KEY_SUFFIX = ":tokens";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final RedisScript<Long> deleteRefreshTokenScript;

    public RefreshTokenRedisAdapter(final RedisTemplate<String, String> redisTemplate, final JwtProperties jwtProperties) {

        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.deleteRefreshTokenScript = RedisScript.of(
            new ClassPathResource("scripts/delete_refresh_token.lua"),
            Long.class
        );
    }

    @Override
    public TokenValidationResult validate(final String token) {

        final String accountIdValue = redisTemplate.opsForValue().get(refreshTokenKey(token));

        return accountIdValue == null
            ? new TokenValidationResult.Invalid()
            : new TokenValidationResult.Valid(new AccountId(UUID.fromString(accountIdValue)));
    }

    @Override
    public void save(final String token, final AccountId accountId) {

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            final byte[] refreshTokenKey = refreshTokenKey(token).getBytes();
            final byte[] accountIdBytes = accountId.value().toString().getBytes();
            final byte[] userTokensKey = userTokensKey(accountId).getBytes();
            final byte[] tokenBytes = token.getBytes();
            connection.stringCommands().set(refreshTokenKey, accountIdBytes, Expiration.seconds(jwtProperties.refreshTokenTtlSeconds()), SetOption.UPSERT);
            connection.setCommands().sAdd(userTokensKey, tokenBytes);
            return null;
        });
    }

    @Override
    public void delete(final String token) {

        redisTemplate.execute(deleteRefreshTokenScript, List.of(refreshTokenKey(token)), token);
    }

    private String refreshTokenKey(final String token) {

        return REFRESH_TOKEN_KEY_PREFIX + token;
    }

    private String userTokensKey(final AccountId accountId) {

        return USER_TOKENS_KEY_PREFIX + accountId.value() + USER_TOKENS_KEY_SUFFIX;
    }
}
