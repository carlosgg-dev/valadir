package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpRepository;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.HashedOtp;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class OtpRepositoryRedisAdapter implements OtpRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final UnaryOperator<String> redisKeyFunction;

    public OtpRepositoryRedisAdapter(RedisTemplate<String, String> redisTemplate, UnaryOperator<String> redisKeyFunction) {

        this.redisTemplate = redisTemplate;
        this.redisKeyFunction = redisKeyFunction;
    }

    @Override
    public void save(AccountId accountId, HashedOtp hashedOtp, Duration ttl) {

        try {
            redisTemplate.opsForValue().set(redisKey(accountId), hashedOtp.value(), ttl);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — otp save failed", e);
        }
    }

    @Override
    public Optional<HashedOtp> find(AccountId accountId) {

        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(accountId)))
                .map(HashedOtp::new);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — otp lookup failed", e);
        }
    }

    @Override
    public void delete(AccountId accountId) {

        try {
            redisTemplate.delete(redisKey(accountId));
        } catch (DataAccessException e) {
            throw new InfrastructureException("Redis unavailable — otp delete failed", e);
        }
    }

    private String redisKey(AccountId accountId) {

        return redisKeyFunction.apply(accountId.value().toString());
    }
}
