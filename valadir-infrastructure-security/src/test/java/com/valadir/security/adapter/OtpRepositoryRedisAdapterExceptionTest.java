package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.test.mother.OtpMother;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class OtpRepositoryRedisAdapterExceptionTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final UnaryOperator<String> REDIS_KEY_FN = id -> "test:otp:" + id;

    @Test
    void save_redisError_throwsInfrastructureException() {

        var hashedOtp = OtpMother.hashed();
        var adapter = new OtpRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT_ID, hashedOtp, OTP_TTL))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void find_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void delete_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), REDIS_KEY_FN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
