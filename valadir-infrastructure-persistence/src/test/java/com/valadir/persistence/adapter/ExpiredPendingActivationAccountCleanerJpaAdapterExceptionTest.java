package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.repository.AccountJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExpiredPendingActivationAccountCleanerJpaAdapterExceptionTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @InjectMocks
    private ExpiredPendingActivationAccountCleanerJpaAdapter adapter;

    @Test
    void delete_postgresError_throwsInfrastructureException() {

        var now = Instant.now();
        
        given(accountJpaRepository.deleteByStatusOlderThan(any(AccountStatus.class), any(Instant.class)))
            .willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(now))
            .withCause(POSTGRES_ERROR);
    }
}
