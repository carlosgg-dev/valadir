package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.UUID;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AccessTokenRevocationRedisAdapterExceptionTest {

    @Test
    void isRevoked_redisError_throwsInfrastructureException() {

        var adapter = new AccessTokenRevocationRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

        String jti = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.isRevoked(jti, accountId, issuedAt))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
