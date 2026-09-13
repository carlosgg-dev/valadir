package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class UpdateProfilePersistenceJpaAdapterTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    private static final Account ACCOUNT = AccountMother.active().build();

    private static final User USER = UserMother.builder()
        .withAccountId(ACCOUNT.getId())
        .build();

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UpdateProfilePersistenceJpaAdapter adapter;

    @Test
    void update_postgresErrorWritingTheLanguage_throwsInfrastructureException() {

        willThrow(POSTGRES_ERROR)
            .given(accountJpaRepository).updateLanguageById(ACCOUNT.getId().value(), ACCOUNT.getLanguage());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.update(ACCOUNT, USER))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void update_postgresErrorWritingTheNames_throwsInfrastructureException() {

        willThrow(POSTGRES_ERROR)
            .given(userJpaRepository).updateNamesByAccountId(
                USER.getAccountId().value(),
                USER.getFullName().value(),
                USER.getGivenName().value()
            );

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.update(ACCOUNT, USER))
            .withCause(POSTGRES_ERROR);
    }
}
