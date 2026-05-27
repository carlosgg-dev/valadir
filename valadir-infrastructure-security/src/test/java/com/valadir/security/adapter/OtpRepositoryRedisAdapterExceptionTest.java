package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.HashedOtp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class OtpRepositoryRedisAdapterExceptionTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final HashedOtp HASHED_OTP = new HashedOtp("$argon2id$hashedOtp");
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final UnaryOperator<String> REDIS_KEY_FN = id -> "test:otp:" + id;

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    @SuppressWarnings("unchecked")
    private static RedisOperations<String, String> redisErrorTemplate() {

        return (RedisOperations<String, String>) Proxy.newProxyInstance(
            RedisOperations.class.getClassLoader(),
            new Class[]{RedisOperations.class},
            (proxy, method, args) -> {throw REDIS_ERROR;}
        );
    }

    @Test
    void save_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT_ID, HASHED_OTP, OTP_TTL))
            .withCause(REDIS_ERROR);
    }

    @Test
    void find_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }

    @Test
    void delete_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }
}
