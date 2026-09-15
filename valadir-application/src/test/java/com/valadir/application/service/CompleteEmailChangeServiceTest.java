package com.valadir.application.service;

import com.valadir.application.command.CompleteEmailChangeCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.ChangeEmailPersistence;
import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.application.port.out.EmailChangeRequestRepository;
import com.valadir.application.port.out.EmailChangedNotifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.OtpMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CompleteEmailChangeServiceTest {

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Infrastructure error");

    // Deliberately not the fallback language: an owner notified in EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    private static final Email NEW_EMAIL = Email.from("matches.malone@email.com");
    private static final PlainOtp CODE = OtpMother.plain();
    private static final HashedOtp HASHED_OTP = OtpMother.hashed();

    private static final CompleteEmailChangeCommand COMMAND = new CompleteEmailChangeCommand(
        ACCOUNT.getId().value().toString(),
        CODE.value()
    );

    @Mock
    private EmailChangeRequestRepository emailChangeRequestRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailHolderResolver emailHolderResolver;

    @Mock
    private ChangeEmailPersistence changeEmailPersistence;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private EmailChangedNotifier emailChangedNotifier;

    @InjectMocks
    private CompleteEmailChangeService service;

    @Test
    void complete_validCodeForAFreeEmail_changesTheEmailAndTellsThePreviousAddress() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());

        service.complete(COMMAND);

        then(changeEmailPersistence).should().change(ACCOUNT.getId(), NEW_EMAIL);
        then(changeEmailPersistence).should(never()).changeReplacing(any(), any(), any());
        then(emailChangeRequestRepository).should().delete(ACCOUNT.getId());
        then(loginAttemptRepository).should().clearAttempts(ACCOUNT.getEmail());
        then(emailChangedNotifier).should().notifyEmailChanged(ACCOUNT.getEmail(), ACCOUNT_LANGUAGE);
    }

    @Test
    void complete_validCodeForAnEmailHeldByAPendingAccount_replacesThatAccount() {

        var pendingAccountId = AccountId.generate();

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.of(pendingAccountId));

        service.complete(COMMAND);

        then(changeEmailPersistence).should().changeReplacing(pendingAccountId, ACCOUNT.getId(), NEW_EMAIL);
        then(changeEmailPersistence).should(never()).change(any(), any());
    }

    @Test
    void complete_emailActivatedByAnotherAccountMeanwhile_propagatesTheConflictWithoutChangingAnything() {

        var conflict = new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS);

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willThrow(conflict);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(conflict);

        then(changeEmailPersistence).shouldHaveNoInteractions();
        then(emailChangeRequestRepository).should(never()).delete(any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(emailChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void complete_noPendingRequest_throwsInvalidEmailChangeOtpWithoutChangingAnything() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP);

        then(changeEmailPersistence).shouldHaveNoInteractions();
        then(emailChangeRequestRepository).should(never()).delete(any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(emailChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void complete_wrongCode_throwsInvalidEmailChangeOtpWithoutChangingAnything() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP);

        then(changeEmailPersistence).shouldHaveNoInteractions();
        then(emailChangeRequestRepository).should(never()).delete(any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(emailChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void complete_malformedCode_translatesToApplicationException() {

        var command = new CompleteEmailChangeCommand(ACCOUNT.getId().value().toString(), "12ab56");

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_OTP);
    }

    @Test
    void complete_accountNotFound_throwsApplicationExceptionWithoutChangingAnything() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(changeEmailPersistence).shouldHaveNoInteractions();
        then(emailChangeRequestRepository).should(never()).delete(any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(emailChangedNotifier).shouldHaveNoInteractions();
    }

    // The request survives the 503, so the owner retries with the same code
    @Test
    void complete_emailPersistenceFails_propagatesAndKeepsTheRequest() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        willThrow(INFRA_ERROR).given(changeEmailPersistence).change(ACCOUNT.getId(), NEW_EMAIL);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.complete(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(emailChangeRequestRepository).should(never()).delete(any());
        then(loginAttemptRepository).shouldHaveNoInteractions();
        then(emailChangedNotifier).shouldHaveNoInteractions();
    }

    @Test
    void complete_requestCleanupFails_stillCompletes() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        willThrow(INFRA_ERROR).given(emailChangeRequestRepository).delete(ACCOUNT.getId());

        assertThatNoException().isThrownBy(() -> service.complete(COMMAND));

        then(loginAttemptRepository).should().clearAttempts(ACCOUNT.getEmail());
        then(emailChangedNotifier).should().notifyEmailChanged(ACCOUNT.getEmail(), ACCOUNT_LANGUAGE);
    }

    @Test
    void complete_notificationFails_stillCompletes() {

        given(emailChangeRequestRepository.find(ACCOUNT.getId())).willReturn(Optional.of(new EmailChangeRequest(NEW_EMAIL, HASHED_OTP)));
        given(otpHasher.matches(CODE, HASHED_OTP)).willReturn(true);
        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        willThrow(INFRA_ERROR).given(emailChangedNotifier).notifyEmailChanged(ACCOUNT.getEmail(), ACCOUNT_LANGUAGE);

        assertThatNoException().isThrownBy(() -> service.complete(COMMAND));
    }
}
