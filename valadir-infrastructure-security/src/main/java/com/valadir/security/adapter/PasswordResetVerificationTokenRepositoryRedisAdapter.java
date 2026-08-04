package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.core.RedisOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class PasswordResetVerificationTokenRepositoryRedisAdapter implements PasswordResetVerificationTokenRepository {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;

    public PasswordResetVerificationTokenRepositoryRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
    }

    @Override
    public void save(String verificationToken, AccountId accountId, Duration ttl) {

        circuitGuard.run("password reset OTP verification save failed", () ->
            redisOperations.opsForValue().set(
                redisKey(verificationToken),
                accountId.value().toString(),
                ttl
            )
        );
    }

    @Override
    public Optional<AccountId> resolveAccountId(String verificationToken) {

        return circuitGuard.call("password reset OTP verification lookup failed", () ->
            Optional.ofNullable(redisOperations.opsForValue().get(redisKey(verificationToken)))
                .map(accountIdValue -> AccountId.from(UUID.fromString(accountIdValue)))
        );
    }

    @Override
    public void delete(String verificationToken) {

        circuitGuard.run("password reset OTP verification delete failed", () ->
            redisOperations.delete(redisKey(verificationToken))
        );
    }

    private String redisKey(String verificationToken) {

        return RedisKeySpace.forPasswordResetVerificationToken(verificationToken);
    }
}
