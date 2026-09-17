package com.valadir.application.service;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.PasswordHasher;
import com.valadir.application.port.out.PasswordResetVerification;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CompletePasswordResetServiceTest {

    private static final Account ACCOUNT = AccountMother.active().build();
    private static final RawPassword NEW_PASSWORD = PasswordMother.raw();
    private static final HashedPassword NEW_HASHED_PASSWORD = PasswordMother.hashed();
    private static final String VERIFICATION_TOKEN = UUID.randomUUID().toString();

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Infrastructure error");
    private static final PasswordResetVerification VERIFICATION = new PasswordResetVerification(ACCOUNT.getId(), ACCOUNT.getEmail());

    private static final CompletePasswordResetCommand COMMAND = new CompletePasswordResetCommand(
        VERIFICATION_TOKEN,
        NEW_PASSWORD.value()
    );

    @Mock
    private PasswordResetVerificationTokenRepository verificationTokenRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NewPasswordValidator newPasswordValidator;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AccountTokensInvalidator accountTokensInvalidator;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private CompletePasswordResetService service;

    @Test
    void complete_validToken_revokesSessionsBeforeWritingThePasswordAndSpendsTheTokenLast() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);

        service.complete(COMMAND);

        InOrder order = inOrder(newPasswordValidator, accountTokensInvalidator, accountRepository, loginAttemptRepository, verificationTokenRepository);
        then(newPasswordValidator).should(order).validate(ACCOUNT, NEW_PASSWORD);
        then(accountTokensInvalidator).should(order).invalidateAll(ACCOUNT.getId());
        then(accountRepository).should(order).updatePassword(ACCOUNT.getId(), NEW_HASHED_PASSWORD);
        then(loginAttemptRepository).should(order).clearAttempts(ACCOUNT.getEmail());
        then(verificationTokenRepository).should(order).delete(VERIFICATION_TOKEN);
    }

    @Test
    void complete_tokenNotFound_throwsApplicationExceptionWithoutChangingAnything() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN);

        then(accountTokensInvalidator).should(never()).invalidateAll(any());
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    @Test
    void complete_accountNotFound_throwsApplicationExceptionWithoutChangingAnything() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountTokensInvalidator).should(never()).invalidateAll(any());
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    // Whoever read the code in the old mailbox must not reset the password once the account has moved on.
    @Test
    void complete_emailChangedSinceVerification_throwsWithoutChangingAnything() {

        var staleVerification = new PasswordResetVerification(ACCOUNT.getId(), Email.from("old.address@email.com"));

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(staleVerification));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN);

        then(accountTokensInvalidator).should(never()).invalidateAll(any());
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    @Test
    void complete_insecurePassword_translatesDomainExceptionWithoutChangingAnything() {

        var rejection = new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        willThrow(rejection).given(newPasswordValidator).validate(ACCOUNT, NEW_PASSWORD);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .withCause(rejection)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INSECURE_PASSWORD);

        then(accountTokensInvalidator).should(never()).invalidateAll(any());
        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    // Swallowed, it would answer 204 with every session still alive.
    @Test
    void complete_sessionRevocationFails_propagatesWithoutWritingThePasswordOrSpendingTheToken() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(accountTokensInvalidator).invalidateAll(ACCOUNT.getId());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    // Swallowed, it would answer 204 over the old password with the token already spent.
    @Test
    void complete_passwordUpdateFails_propagatesWithoutSpendingTheToken() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(accountRepository).updatePassword(ACCOUNT.getId(), NEW_HASHED_PASSWORD);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(loginAttemptRepository).should(never()).clearAttempts(any());
        then(verificationTokenRepository).should(never()).delete(any());
    }

    // Swallowed, it would answer 204 with the lockout still standing and the token already spent.
    @Test
    void complete_clearingAttemptsFails_propagatesWithoutSpendingTheToken() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(loginAttemptRepository).clearAttempts(ACCOUNT.getEmail());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(verificationTokenRepository).should(never()).delete(any());
    }

    // Swallowed, it would answer 204 with the token still spendable.
    @Test
    void complete_tokenDeletionFails_propagates() {

        given(verificationTokenRepository.verificationFor(VERIFICATION_TOKEN)).willReturn(Optional.of(VERIFICATION));
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(NEW_HASHED_PASSWORD);
        willThrow(INFRA_ERROR).given(verificationTokenRepository).delete(VERIFICATION_TOKEN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(INFRA_ERROR);
    }
}
