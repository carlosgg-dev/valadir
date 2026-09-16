package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
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
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ExpiredPendingActivationAccountCleanerJpaAdapterTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private ExpiredPendingActivationAccountCleanerJpaAdapter adapter;

    @Test
    void delete_postgresErrorRemovingTheProfiles_throwsInfrastructureExceptionAndKeepsTheAccounts() {

        var now = Instant.now();

        willThrow(POSTGRES_ERROR).given(userJpaRepository)
            .deleteByAccountStatusOlderThan(AccountStatus.PENDING_ACTIVATION, now);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(now))
            .withCause(POSTGRES_ERROR);

        then(accountJpaRepository).should(never()).deleteByStatusOlderThan(any(AccountStatus.class), any(Instant.class));
    }

    @Test
    void delete_postgresErrorRemovingTheAccounts_throwsInfrastructureException() {

        var now = Instant.now();

        given(accountJpaRepository.deleteByStatusOlderThan(any(AccountStatus.class), any(Instant.class)))
            .willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(now))
            .withCause(POSTGRES_ERROR);
    }
}
