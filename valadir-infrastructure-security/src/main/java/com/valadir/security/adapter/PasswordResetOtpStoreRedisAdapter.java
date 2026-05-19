package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetOtpStore;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class PasswordResetOtpStoreRedisAdapter implements PasswordResetOtpStore {

    private final RedisTemplate<String, String> redisTemplate;

    public PasswordResetOtpStoreRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(AccountId accountId, String hashedOtp, Duration ttl) {

        try {
            redisTemplate.opsForValue().set(redisKey(accountId), hashedOtp, ttl);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP save failed", e);
        }
    }

    @Override
    public Optional<String> find(AccountId accountId) {

        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(accountId)));
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP lookup failed", e);
        }
    }

    @Override
    public void delete(AccountId accountId) {

        try {
            redisTemplate.delete(redisKey(accountId));
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — password reset OTP delete failed", e);
        }
    }

    private String redisKey(AccountId accountId) {

        return RedisKeySpace.forPasswordResetOtp(accountId.value().toString());
    }
}
