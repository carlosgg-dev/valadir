package com.valadir.application.service;

import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.DeleteAccountPersistence;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeleteAccountServiceTest {

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Infrastructure error");

    private static final Account ACCOUNT = AccountMother.active().build();

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountReauthenticator accountReauthenticator;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private AccountTokensInvalidator accountTokensInvalidator;

    @Mock
    private DeleteAccountPersistence deleteAccountPersistence;

    @InjectMocks
    private DeleteAccountService service;

    @Test
    void delete_correctPassword_reauthenticatesThenRevokesSessionsBeforeDeletingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));

        service.delete(command);

        InOrder order = inOrder(accountReauthenticator, accountTokensInvalidator, deleteAccountPersistence);
        then(accountReauthenticator).should(order).reauthenticate(ACCOUNT, password);
        then(accountTokensInvalidator).should(order).invalidateAll(ACCOUNT.getId());
        then(deleteAccountPersistence).should(order).delete(ACCOUNT.getId());

        then(loginAttemptRepository).should().clearAttempts(ACCOUNT.getEmail());
    }

    // Lockout and wrong password alike: whatever the re-authentication refuses, nothing is revoked or deleted.
    @Test
    void delete_reauthenticationRefused_propagatesWithoutTouchingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());
        var refusal = new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        willThrow(refusal).given(accountReauthenticator).reauthenticate(ACCOUNT, password);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .isSameAs(refusal);

        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(deleteAccountPersistence).shouldHaveNoInteractions();
        then(loginAttemptRepository).shouldHaveNoInteractions();
    }

    @Test
    void delete_accountNotFound_throwsApplicationExceptionWithoutRevokingSessions() {

        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), PasswordMother.raw().value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountReauthenticator).shouldHaveNoInteractions();
        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    // Swallowing it would answer 204 over an account whose sessions all stay alive.
    @Test
    void delete_sessionRevocationFails_propagatesWithoutDeletingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));

        willThrow(INFRA_ERROR).given(accountTokensInvalidator).invalidateAll(ACCOUNT.getId());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.delete(command))
            .isSameAs(INFRA_ERROR);

        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    // The sessions are already gone, but the account is not: the 503 is what tells the owner to retry.
    @Test
    void delete_accountDeletionFails_propagatesInfrastructureException() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));

        willThrow(INFRA_ERROR).given(deleteAccountPersistence).delete(ACCOUNT.getId());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.delete(command))
            .isSameAs(INFRA_ERROR);

        then(loginAttemptRepository).should(never()).clearAttempts(any());
    }

    @Test
    void delete_malformedPassword_translatesToApplicationException() {

        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), "invalid-password");

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_PASSWORD);

        then(accountRepository).should(never()).findById(any());
    }
}
