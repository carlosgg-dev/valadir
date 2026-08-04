package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.HashedOtp;
import com.valadir.security.redis.RedisCircuitGuard;
import org.springframework.data.redis.core.RedisOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class OtpRepositoryRedisAdapter implements OtpRepository {

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final UnaryOperator<String> redisKeyFunction;

    public OtpRepositoryRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard,
        UnaryOperator<String> redisKeyFunction
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.redisKeyFunction = redisKeyFunction;
    }

    @Override
    public void save(AccountId accountId, HashedOtp hashedOtp, Duration ttl) {

        circuitGuard.run("otp save failed", () ->
            redisOperations.opsForValue().set(redisKey(accountId), hashedOtp.value(), ttl)
        );
    }

    @Override
    public Optional<HashedOtp> find(AccountId accountId) {

        return circuitGuard.call("otp lookup failed", () ->
            Optional.ofNullable(redisOperations.opsForValue().get(redisKey(accountId)))
                .map(HashedOtp::new)
        );
    }

    @Override
    public void delete(AccountId accountId) {

        circuitGuard.run("otp delete failed", () -> redisOperations.delete(redisKey(accountId)));
    }

    private String redisKey(AccountId accountId) {

        return redisKeyFunction.apply(accountId.value().toString());
    }
}
