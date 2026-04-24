package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpStore;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class OtpRedisAdapter implements OtpStore {

    private final RedisTemplate<String, String> redisTemplate;

    public OtpRedisAdapter(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(AccountId accountId, String hashedOtp, Duration ttl) {

        try {
            redisTemplate.opsForValue().set(key(accountId), hashedOtp, ttl);
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — otp save failed", e);
        }
    }

    @Override
    public Optional<String> find(AccountId accountId) {

        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(accountId)));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — otp lookup failed", e);
        }
    }

    @Override
    public void delete(AccountId accountId) {

        try {
            redisTemplate.delete(key(accountId));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            throw new InfrastructureException("Redis unavailable — otp delete failed", e);
        }
    }

    private String key(AccountId accountId) {

        return RedisKeySpace.forVerificationOtp(accountId.value().toString());
    }
}
