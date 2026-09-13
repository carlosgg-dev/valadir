package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class DeleteAccountPersistenceJpaAdapterTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private DeleteAccountPersistenceJpaAdapter adapter;

    @Test
    void delete_postgresErrorRemovingTheProfile_throwsInfrastructureException() {

        var accountId = AccountId.generate();

        willThrow(POSTGRES_ERROR).given(userJpaRepository).deleteByAccountId(accountId.value());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(accountId))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void delete_postgresErrorFlushingTheAccountDeletion_throwsInfrastructureException() {

        var accountId = AccountId.generate();

        // The flush is where the account's DELETE actually reaches Postgres.
        willThrow(POSTGRES_ERROR).given(accountJpaRepository).flush();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(accountId))
            .withCause(POSTGRES_ERROR);
    }
}
