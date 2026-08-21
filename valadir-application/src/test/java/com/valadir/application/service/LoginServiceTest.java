package com.valadir.application.service;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.CaptchaVerifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.service.PasswordHasher;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final Account EXISTING_ACCOUNT = AccountMother.active()
        .withEmail(Email.from("bruce.wayne@email.com"))
        .build();

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private CaptchaVerifier captchaVerifier;

    @Mock
    private AccountLockedNotifier accountLockedNotifier;

    @InjectMocks
    private LoginService service;

    @Test
    void login_validCredentials_returnsTokensAndResetsAttemptState() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var accessToken = "access-token";
        var refreshToken = "refresh-token";
        var command = new LoginCommand(email.value(), password.value(), null);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.Allowed());
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(true);
        given(authTokenIssuer.issue(EXISTING_ACCOUNT.getId(), EXISTING_ACCOUNT.getRole()))
            .willReturn(new AuthTokenResult(accessToken, refreshToken));

        AuthTokenResult result = service.login(command);

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);

        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
        then(loginAttemptRepository).should().clearAttempts(email);
        then(refreshTokenRepository).should().save(refreshToken, EXISTING_ACCOUNT.getId());
    }

    @Test
    void login_withActiveLockout_throwsAccountLockedException() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var remainingLockout = Duration.ofSeconds(30);
        var command = new LoginCommand(email.value(), password.value(), null);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.LockedOut(remainingLockout));

        assertThatExceptionOfType(AccountLockedException.class)
            .isThrownBy(() -> service.login(command))
            .satisfies(exception -> assertThat(exception.lockout()).isEqualTo(remainingLockout));

        verifyNoInteractions(accountRepository, passwordHasher, authTokenIssuer);
    }

    @Test
    void login_challengeRequiredWithInvalidCaptcha_throwsCaptchaRequiredAndSkipsPassword() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var captchaToken = "invalid-token";
        var command = new LoginCommand(email.value(), password.value(), captchaToken);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.ChallengeRequired());
        given(captchaVerifier.isValid(captchaToken)).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CAPTCHA_REQUIRED);

        verifyNoInteractions(accountRepository, passwordHasher, authTokenIssuer);
    }

    @Test
    void login_challengeRequiredWithValidCaptcha_proceedsToPasswordCheck() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var captchaToken = "valid-token";
        var command = new LoginCommand(email.value(), password.value(), captchaToken);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.ChallengeRequired());
        given(captchaVerifier.isValid(captchaToken)).willReturn(true);
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(true);
        given(authTokenIssuer.issue(EXISTING_ACCOUNT.getId(), EXISTING_ACCOUNT.getRole()))
            .willReturn(new AuthTokenResult("access-token", "refresh-token"));

        service.login(command);

        // The behavior under test is the delegation itself: a satisfied challenge lets the
        // flow reach the credential check. The login outcome is covered by other tests.
        then(passwordHasher).should().matches(password, EXISTING_ACCOUNT.getPassword());
    }

    @Test
    void login_unknownEmail_recordsAttemptAndThrowsWithoutNotifyingOwner() {

        var email = Email.from("unknown@email.com");
        var password = PasswordMother.raw();
        var command = new LoginCommand(email.value(), password.value(), null);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.Allowed());
        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(passwordHasher).should().decoyMatch(password);
        then(loginAttemptRepository).should().recordFailedAttempt(email);
        then(accountLockedNotifier).shouldHaveNoInteractions();
        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_wrongPasswordWithoutLockout_recordsAttemptAndThrowsWithoutNotifyingOwner() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var command = new LoginCommand(email.value(), password.value(), null);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.Allowed());
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(loginAttemptRepository).should().recordFailedAttempt(email);
        then(accountLockedNotifier).shouldHaveNoInteractions();
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_wrongPasswordEstablishingLockout_notifiesOwnerOnce() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var command = new LoginCommand(email.value(), password.value(), null);
        var lockout = Duration.ofMinutes(5);

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.Allowed());
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(false);
        given(loginAttemptRepository.recordFailedAttempt(email)).willReturn(Optional.of(lockout));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(accountLockedNotifier).should().notifyAccountLocked(email, lockout);
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_accountPendingActivation_throwsWithoutRecordingAttempt() {

        var email = Email.from("bruce.wayne@email.com");
        var password = PasswordMother.raw();
        var command = new LoginCommand(email.value(), password.value(), null);
        var pendingAccount = AccountMother.pendingActivation().withEmail(email).build();

        given(loginAttemptRepository.evaluate(email)).willReturn(new LoginAttemptDecision.Allowed());
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        given(passwordHasher.matches(password, pendingAccount.getPassword())).willReturn(true);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PENDING_ACTIVATION);

        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_domainException_translatesToApplicationException() {

        var command = new LoginCommand("not-an-email", PasswordMother.raw().value(), null);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FIELD);

        then(accountRepository).should(never()).findByEmail(any());
    }
}
