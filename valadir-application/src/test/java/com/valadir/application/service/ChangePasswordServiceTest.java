package com.valadir.application.service;

import com.valadir.application.command.ChangePasswordCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.PasswordChangedNotifier;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;
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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Infrastructure error");

    // Deliberately not the fallback language: an owner notified in EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    private static final RawPassword CURRENT_PASSWORD = PasswordMother.raw();
    private static final RawPassword NEW_PASSWORD = RawPassword.from("AnotherP@ss456");
    private static final HashedPassword NEW_HASHED_PASSWORD = PasswordMother.hashed();

    private static final ChangePasswordCommand COMMAND = new ChangePasswordCommand(
        ACCOUNT.getId().value().toString(),
        CURRENT_PASSWORD.value(),
        NEW_PASSWORD.value()
    );

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NewPasswordValidator newPasswordValidator;

    @Mock
    private AccountReauthenticator accountReauthenticator;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AccountTokensInvalidator accountTokensInvalidator;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private PasswordChangedNotifier passwordChangedNotifier;

    @InjectMocks
    private ChangePasswordService service;

    @Test
    void change_correctCurrentPassword_revokesSessionsBeforeWritingTheNewPassword() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);

        service.change(COMMAND);

        InOrder order = inOrder(accountReauthenticator, newPasswordValidator, accountTokensInvalidator, accountRepository);
        then(accountReauthenticator).should(order).reauthenticate(ACCOUNT, CURRENT_PASSWORD);
        then(newPasswordValidator).should(order).validate(ACCOUNT, NEW_PASSWORD);
        then(accountTokensInvalidator).should(order).invalidateAll(ACCOUNT.getId());
        then(accountRepository).should(order).updatePassword(ACCOUNT.getId(), NEW_HASHED_PASSWORD);

        then(loginAttemptRepository).should().clearAttempts(ACCOUNT.getEmail());
        then(passwordChangedNotifier).should().notifyPasswordChanged(ACCOUNT.getEmail(), ACCOUNT_LANGUAGE);
    }

    @Test
    void change_malformedNewPassword_translatesToApplicationException() {

        var command = new ChangePasswordCommand(ACCOUNT.getId().value().toString(), CURRENT_PASSWORD.value(), "invalid-password");

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.change(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void change_accountNotFound_throwsApplicationExceptionWithoutChangingAnything() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.change(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(passwordChangedNotifier).shouldHaveNoInteractions();
    }

    // Lockout and wrong password alike: whatever the re-authentication refuses, nothing is revoked or written.
    @Test
    void change_reauthenticationRefused_propagatesWithoutChangingAnything() {

        var refusal = new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        willThrow(refusal).given(accountReauthenticator).reauthenticate(ACCOUNT, CURRENT_PASSWORD);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.change(COMMAND))
            .isSameAs(refusal);

        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(passwordChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void change_insecureNewPassword_throwsWithoutChangingAnything() {

        var rejection = new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        willThrow(rejection).given(newPasswordValidator).validate(ACCOUNT, NEW_PASSWORD);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.change(COMMAND))
            .withCause(rejection)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INSECURE_PASSWORD);

        then(accountTokensInvalidator).shouldHaveNoInteractions();
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(passwordChangedNotifier).shouldHaveNoInteractions();
    }

    // Swallowing it would answer 204 over a new password with every old session still alive.
    @Test
    void change_sessionRevocationFails_propagatesWithoutWritingThePassword() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(accountTokensInvalidator).invalidateAll(ACCOUNT.getId());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.change(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(passwordChangedNotifier).shouldHaveNoInteractions();
    }

    // The sessions are gone but the old password still opens the account: the 503 is what tells the owner
    // to sign back in and retry.
    @Test
    void change_passwordUpdateFails_propagatesWithoutNotifyingTheOwner() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(accountRepository).updatePassword(ACCOUNT.getId(), NEW_HASHED_PASSWORD);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.change(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(passwordChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void change_notificationFails_stillCompletes() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(passwordChangedNotifier).notifyPasswordChanged(ACCOUNT.getEmail(), ACCOUNT_LANGUAGE);

        assertThatNoException().isThrownBy(() -> service.change(COMMAND));
    }
}
