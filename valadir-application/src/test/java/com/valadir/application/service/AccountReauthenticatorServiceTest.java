package com.valadir.application.service;

import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.PasswordHasher;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Language;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AccountReauthenticatorServiceTest {

    // Deliberately not the fallback language: an owner notified in EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private AccountLockedNotifier accountLockedNotifier;

    @InjectMocks
    private AccountReauthenticatorService reauthenticator;

    @Test
    void reauthenticate_correctPassword_passesWithoutRecordingAFailure() {

        var password = PasswordMother.raw();

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getHashedPassword())).willReturn(true);

        assertThatNoException().isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password));

        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
    }

    // The shared counter can reach the challenge threshold, but the step-up belongs to the anonymous
    // login: past it, an authenticated caller with the right password still gets through
    @Test
    void reauthenticate_challengeRequired_ignoresTheStepUp() {

        var password = PasswordMother.raw();

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.ChallengeRequired());
        given(passwordHasher.matches(password, ACCOUNT.getHashedPassword())).willReturn(true);

        assertThatNoException().isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password));
    }

    @Test
    void reauthenticate_wrongPasswordWithoutLockout_recordsAttemptAndThrowsCredentialIntegrityError() {

        var password = PasswordMother.raw();

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getHashedPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(accountLockedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void reauthenticate_wrongPasswordEstablishingLockout_notifiesOwner() {

        var password = PasswordMother.raw();
        var lockout = Duration.ofMinutes(5);

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getHashedPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.of(lockout));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(accountLockedNotifier).should().notifyAccountLocked(ACCOUNT.getEmail(), lockout, ACCOUNT_LANGUAGE);
    }

    @Test
    void reauthenticate_lockoutNotificationFails_stillThrowsApplicationException() {

        var password = PasswordMother.raw();
        var lockout = Duration.ofMinutes(5);

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.Allowed());
        given(passwordHasher.matches(password, ACCOUNT.getHashedPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(ACCOUNT.getEmail())).willReturn(Optional.of(lockout));

        willThrow(new InfrastructureException("Infrastructure error"))
            .given(accountLockedNotifier).notifyAccountLocked(ACCOUNT.getEmail(), lockout, ACCOUNT_LANGUAGE);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
    }

    // Even the right password: a locked account is closed to whoever is guessing at it.
    @Test
    void reauthenticate_activeLockout_throwsAccountLockedExceptionWithoutCheckingThePassword() {

        var remainingLockout = Duration.ofSeconds(30);
        var password = PasswordMother.raw();

        given(loginAttemptRepository.decisionFor(ACCOUNT.getEmail())).willReturn(new LoginAttemptDecision.LockedOut(remainingLockout));

        assertThatExceptionOfType(AccountLockedException.class)
            .isThrownBy(() -> reauthenticator.reauthenticate(ACCOUNT, password))
            .satisfies(exception -> assertThat(exception.lockout()).isEqualTo(remainingLockout));

        then(passwordHasher).shouldHaveNoInteractions();
        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
    }
}
