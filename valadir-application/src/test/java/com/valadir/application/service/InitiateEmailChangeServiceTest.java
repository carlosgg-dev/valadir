package com.valadir.application.service;

import com.valadir.application.command.InitiateEmailChangeCommand;
import com.valadir.application.config.EmailChangeConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.EmailChangeNotifier;
import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.application.port.out.EmailChangeRequestRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;
import com.valadir.domain.model.RawPassword;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.OtpMother;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

@ExtendWith(MockitoExtension.class)
class InitiateEmailChangeServiceTest {

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Infrastructure error");

    // Deliberately not the fallback language: a code sent in EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    private static final Email NEW_EMAIL = Email.from("matches.malone@email.com");
    private static final RawPassword PASSWORD = PasswordMother.raw();
    private static final HashedOtp HASHED_OTP = OtpMother.hashed();
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    private static final InitiateEmailChangeCommand COMMAND = new InitiateEmailChangeCommand(
        ACCOUNT.getId().value().toString(),
        NEW_EMAIL.value(),
        PASSWORD.value()
    );

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountReauthenticator accountReauthenticator;

    @Mock
    private EmailHolderResolver emailHolderResolver;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private EmailChangeRequestRepository emailChangeRequestRepository;

    @Mock
    private EmailChangeNotifier emailChangeNotifier;

    @Mock
    private EmailChangeConfig emailChangeConfig;

    @InjectMocks
    private InitiateEmailChangeService service;

    @Captor
    private ArgumentCaptor<PlainOtp> plainOtpCaptor;

    @Captor
    private ArgumentCaptor<OtpNotification> notificationCaptor;

    // The code is random, so the one hashed into the request is captured and must be the one mailed.
    @Test
    void initiate_correctPassword_storesTheHashedCodeBeforeMailingItToTheNewEmail() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        given(emailChangeConfig.otpTtl()).willReturn(OTP_TTL);
        given(otpHasher.hash(any(PlainOtp.class))).willReturn(HASHED_OTP);

        service.initiate(COMMAND);

        then(otpHasher).should().hash(plainOtpCaptor.capture());

        InOrder order = inOrder(accountReauthenticator, emailChangeRequestRepository, emailChangeNotifier);
        then(accountReauthenticator)
            .should(order).reauthenticate(ACCOUNT, PASSWORD);
        then(emailChangeRequestRepository)
            .should(order).save(ACCOUNT.getId(), new EmailChangeRequest(NEW_EMAIL, HASHED_OTP), OTP_TTL);
        then(emailChangeNotifier).should(order)
            .sendConfirmationCode(new OtpNotification(NEW_EMAIL, plainOtpCaptor.getValue(), OTP_TTL, ACCOUNT_LANGUAGE));
    }

    // A pending account only claimed the address: it is replaced at completion, not refused here.
    @Test
    void initiate_newEmailHeldByAPendingAccount_stillMailsTheCode() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.of(AccountId.generate()));
        given(emailChangeConfig.otpTtl()).willReturn(OTP_TTL);
        given(otpHasher.hash(any(PlainOtp.class))).willReturn(HASHED_OTP);

        service.initiate(COMMAND);

        then(emailChangeNotifier).should().sendConfirmationCode(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().email()).isEqualTo(NEW_EMAIL);
    }

    @Test
    void initiate_malformedNewEmail_translatesToApplicationException() {

        var command = new InitiateEmailChangeCommand(ACCOUNT.getId().value().toString(), "not-an-email", PASSWORD.value());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.initiate(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_FIELD);
    }

    @Test
    void initiate_accountNotFound_throwsApplicationExceptionWithoutSendingAnything() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.initiate(COMMAND))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(emailChangeRequestRepository).shouldHaveNoInteractions();
        then(emailChangeNotifier).shouldHaveNoInteractions();
    }

    // Lockout and wrong password alike: whatever the re-authentication refuses, no code is stored or sent.
    @Test
    void initiate_reauthenticationRefused_propagatesWithoutSendingAnything() {

        var refusal = new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        willThrow(refusal).given(accountReauthenticator).reauthenticate(ACCOUNT, PASSWORD);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.initiate(COMMAND))
            .isSameAs(refusal);

        then(emailChangeRequestRepository).shouldHaveNoInteractions();
        then(emailChangeNotifier).shouldHaveNoInteractions();
    }

    @Test
    void initiate_newEmailHeldByAnActiveAccount_propagatesTheConflictWithoutSendingAnything() {

        var conflict = new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willThrow(conflict);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.initiate(COMMAND))
            .isSameAs(conflict);

        then(emailChangeRequestRepository).shouldHaveNoInteractions();
        then(emailChangeNotifier).shouldHaveNoInteractions();
    }

    // A code mailed without its request could never be completed.
    @Test
    void initiate_requestStorageFails_propagatesWithoutMailingTheCode() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        given(emailChangeConfig.otpTtl()).willReturn(OTP_TTL);
        given(otpHasher.hash(any(PlainOtp.class))).willReturn(HASHED_OTP);
        willThrow(INFRA_ERROR).given(emailChangeRequestRepository)
            .save(ACCOUNT.getId(), new EmailChangeRequest(NEW_EMAIL, HASHED_OTP), OTP_TTL);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.initiate(COMMAND))
            .isSameAs(INFRA_ERROR);

        then(emailChangeNotifier).shouldHaveNoInteractions();
    }

    // The code never left: a 204 would leave the owner waiting for a mail that is not coming.
    @Test
    void initiate_mailServerUnavailable_propagatesSoTheOwnerRetries() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(emailHolderResolver.replaceableHolderFor(NEW_EMAIL)).willReturn(Optional.empty());
        given(emailChangeConfig.otpTtl()).willReturn(OTP_TTL);
        given(otpHasher.hash(any(PlainOtp.class))).willReturn(HASHED_OTP);
        willThrow(INFRA_ERROR).given(emailChangeNotifier).sendConfirmationCode(any(OtpNotification.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.initiate(COMMAND))
            .isSameAs(INFRA_ERROR);
    }
}
