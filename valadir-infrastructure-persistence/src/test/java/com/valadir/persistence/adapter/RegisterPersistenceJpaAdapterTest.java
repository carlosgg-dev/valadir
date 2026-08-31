package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.entity.AccountEntity;
import com.valadir.persistence.entity.UserEntity;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class RegisterPersistenceJpaAdapterTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    private static final Account ACCOUNT = AccountMother.pendingActivation().build();
    private static final User USER = UserMother.builder().build();

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private RegisterPersistenceJpaAdapter adapter;

    @Test
    void save_postgresErrorOnAccountInsert_throwsInfrastructureException() {

        given(accountJpaRepository.saveAndFlush(any(AccountEntity.class))).willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT, USER))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void save_postgresErrorOnUserInsert_throwsInfrastructureException() {

        given(userJpaRepository.save(any(UserEntity.class))).willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT, USER))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void save_duplicateEmail_stillReportsAConflictRatherThanAnOutage() {

        var duplicateEmail = new DataIntegrityViolationException("unique violation on accounts.email");

        given(accountJpaRepository.saveAndFlush(any(AccountEntity.class))).willThrow(duplicateEmail);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT, USER))
            .withCause(duplicateEmail)
            .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void replace_postgresErrorRemovingTheAbandonedAccount_throwsInfrastructureException() {

        var accountId = AccountId.generate();

        willThrow(POSTGRES_ERROR).given(userJpaRepository).deleteByAccountId(any(UUID.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.replace(accountId, ACCOUNT, USER))
            .withCause(POSTGRES_ERROR);
    }
}
