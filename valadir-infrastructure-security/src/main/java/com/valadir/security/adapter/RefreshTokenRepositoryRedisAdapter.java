package com.valadir.security.adapter;

import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.security.redis.TokenFingerprint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenRepositoryRedisAdapter implements RefreshTokenRepository {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final JwtProperties jwtProperties;
    private final RedisScript<Long> saveRefreshTokenScript;
    private final RedisScript<Long> rotateRefreshTokenScript;

    public RefreshTokenRepositoryRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard,
        JwtProperties jwtProperties
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.jwtProperties = jwtProperties;
        this.saveRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/save_refresh_token.lua"), Long.class);
        this.rotateRefreshTokenScript = RedisScript.of(new ClassPathResource("scripts/rotate_refresh_token.lua"), Long.class);
    }

    @Override
    public Optional<AccountId> validate(String token) {

        var fingerprint = TokenFingerprint.of(token);

        return circuitGuard.call("refresh token validation failed", () ->
            Optional.ofNullable(redisOperations.opsForValue().get(RedisKeySpace.forRefreshToken(fingerprint)))
                .map(value -> AccountId.from(UUID.fromString(value)))
        );
    }

    // Atomic: stores the refresh token and registers it in the user's token set
    @Override
    public void save(String token, AccountId accountId) {

        String accountIdStr = accountId.value().toString();
        var fingerprint = TokenFingerprint.of(token);

        circuitGuard.run("refresh token save failed", () ->
            redisOperations.execute(
                saveRefreshTokenScript,
                List.of(RedisKeySpace.forRefreshToken(fingerprint), RedisKeySpace.forUserTokens(accountIdStr)),
                accountIdStr,
                String.valueOf(jwtProperties.refreshTokenTtl().getSeconds()),
                fingerprint.value()
            )
        );
    }

    // Atomic: removes old token and stores new one with TTL. Returns false if old token no longer exists
    @Override
    public boolean rotate(String oldToken, String newToken, AccountId accountId) {

        String accountIdStr = accountId.value().toString();
        var oldFingerprint = TokenFingerprint.of(oldToken);
        var newFingerprint = TokenFingerprint.of(newToken);

        Long result = circuitGuard.call("refresh token rotation failed", () ->
            redisOperations.execute(
                rotateRefreshTokenScript,
                List.of(
                    RedisKeySpace.forRefreshToken(oldFingerprint),
                    RedisKeySpace.forRefreshToken(newFingerprint),
                    RedisKeySpace.forUserTokens(accountIdStr)
                ),
                oldFingerprint.value(),
                newFingerprint.value(),
                accountIdStr,
                String.valueOf(jwtProperties.refreshTokenTtl().getSeconds())
            )
        );

        return Long.valueOf(1L).equals(result);
    }
}
