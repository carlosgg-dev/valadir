package com.valadir.application.service;

import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.DeleteAccountPersistence;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Language;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    // Deliberately not the fallback language: an owner notified in EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private AccountLockedNotifier accountLockedNotifier;

    @Mock
    private AccountTokensInvalidator accountTokensInvalidator;

    @Mock
    private DeleteAccountPersistence deleteAccountPersistence;

    @InjectMocks
    private DeleteAccountService service;

    @Test
    void delete_correctPassword_revokesSessionsBeforeDeletingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(true);

        service.delete(command);

        InOrder order = inOrder(accountTokensInvalidator, deleteAccountPersistence);
        then(accountTokensInvalidator).should(order).invalidateAll(ACCOUNT.getId());
        then(deleteAccountPersistence).should(order).delete(ACCOUNT.getId());

        then(loginAttemptRepository).should().clearAttempts(ACCOUNT.getEmail());
    }

    // The shared counter can reach the challenge threshold, but the step-up belongs to the anonymous
    // login: past it, an authenticated caller with the right password still deletes
    @Test
    void delete_challengeRequired_ignoresTheStepUpAndDeletes() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.ChallengeRequired());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(true);

        service.delete(command);

        then(deleteAccountPersistence).should().delete(ACCOUNT.getId());
    }

    @Test
    void delete_wrongPasswordWithoutLockout_recordsAttemptAndThrowsWithoutTouchingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(accountLockedNotifier).shouldHaveNoInteractions();
        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(deleteAccountPersistence).shouldHaveNoInteractions();
        then(loginAttemptRepository).should(never()).clearAttempts(any());
    }

    @Test
    void delete_wrongPasswordEstablishingLockout_notifiesOwner() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());
        var lockout = Duration.ofMinutes(5);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.of(lockout));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(accountLockedNotifier).should().notifyAccountLocked(ACCOUNT.getEmail(), lockout, ACCOUNT_LANGUAGE);
        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    @Test
    void delete_lockoutNotificationFails_stillThrowsCredentialIntegrityError() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());
        var lockout = Duration.ofMinutes(5);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.of(lockout));

        willThrow(INFRA_ERROR)
            .given(accountLockedNotifier).notifyAccountLocked(ACCOUNT.getEmail(), lockout, ACCOUNT_LANGUAGE);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    // Even the right password: a locked account cannot be deleted by whoever is guessing at it.
    @Test
    void delete_activeLockout_throwsAccountLockedExceptionWithoutCheckingThePassword() {

        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), PasswordMother.raw().value());
        var remainingLockout = Duration.ofSeconds(30);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.LockedOut(remainingLockout));

        assertThatExceptionOfType(AccountLockedException.class)
            .isThrownBy(() -> service.delete(command))
            .satisfies(exception -> assertThat(exception.lockout()).isEqualTo(remainingLockout));

        then(passwordHasher).shouldHaveNoInteractions();
        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    @Test
    void delete_accountNotFound_throwsDataIntegrityErrorWithoutRevokingSessions() {

        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), PasswordMother.raw().value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.delete(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(deleteAccountPersistence).shouldHaveNoInteractions();
    }

    // Swallowing it would answer 204 over an account whose sessions all stay alive.
    @Test
    void delete_sessionRevocationFails_propagatesWithoutDeletingTheAccount() {

        var password = PasswordMother.raw();
        var command = new DeleteAccountCommand(ACCOUNT.getId().value().toString(), password.value());

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(true);

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
        given(loginAttemptRepository.evaluate(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getPassword())).willReturn(true);

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
