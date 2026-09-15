package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ChangeEmailPersistenceJpaAdapterTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final Email NEW_EMAIL = Email.from("matches.malone@email.com");

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private ChangeEmailPersistenceJpaAdapter adapter;

    @Test
    void change_postgresError_throwsInfrastructureException() {

        willThrow(POSTGRES_ERROR).given(accountJpaRepository).updateEmailById(ACCOUNT_ID.value(), NEW_EMAIL.value());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.change(ACCOUNT_ID, NEW_EMAIL))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void change_duplicateEmail_reportsAConflictRatherThanAnOutage() {

        var duplicateEmail = new DataIntegrityViolationException("unique violation on accounts.email");

        willThrow(duplicateEmail).given(accountJpaRepository).updateEmailById(ACCOUNT_ID.value(), NEW_EMAIL.value());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> adapter.change(ACCOUNT_ID, NEW_EMAIL))
            .withCause(duplicateEmail)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void changeReplacing_postgresErrorRemovingTheAbandonedAccount_throwsInfrastructureException() {

        var abandonedAccountId = AccountId.generate();

        willThrow(POSTGRES_ERROR).given(userJpaRepository).deleteByAccountId(abandonedAccountId.value());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.changeReplacing(abandonedAccountId, ACCOUNT_ID, NEW_EMAIL))
            .withCause(POSTGRES_ERROR);
    }
}
