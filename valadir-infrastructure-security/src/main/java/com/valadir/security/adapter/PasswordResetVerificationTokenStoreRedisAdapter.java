package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetVerificationTokenStore;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class PasswordResetVerificationTokenStoreRedisAdapter implements PasswordResetVerificationTokenStore {

    private final RedisTemplate<String, String> redisTemplate;

    public PasswordResetVerificationTokenStoreRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String verificationToken, AccountId accountId, Duration ttl) {

        try {
            redisTemplate.opsForValue().set(
                redisKey(verificationToken),
                accountId.value().toString(),
                ttl
            );
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP verification save failed", e);
        }
    }

    @Override
    public Optional<AccountId> resolveAccountId(String verificationToken) {

        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(verificationToken)))
                .map(accountIdValue -> new AccountId(UUID.fromString(accountIdValue)));
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP verification lookup failed", e);
        }
    }

    @Override
    public void delete(String verificationToken) {

        try {
            redisTemplate.delete(redisKey(verificationToken));
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP verification delete failed", e);
        }
    }

    private String redisKey(String verificationToken) {

        return RedisKeySpace.forPasswordResetVerificationToken(verificationToken);
    }
}
