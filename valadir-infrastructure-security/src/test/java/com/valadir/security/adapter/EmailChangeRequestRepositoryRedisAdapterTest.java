package com.valadir.security.adapter;

import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.test.mother.OtpMother;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EmailChangeRequestRepositoryRedisAdapterTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    private final EmailChangeRequestRepositoryRedisAdapter adapter =
        new EmailChangeRequestRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

    @Test
    void save_redisError_throwsInfrastructureException() {

        var request = new EmailChangeRequest(Email.from("bruce.wayne@email.com"), OtpMother.hashed());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT_ID, request, OTP_TTL))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void find_redisError_throwsInfrastructureException() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void delete_redisError_throwsInfrastructureException() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
