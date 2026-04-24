package com.valadir.application.service;

import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ResendVerificationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private OtpHasher otpHasher;
    @Mock
    private EmailVerificationPort emailVerificationPort;
    @Mock
    private VerificationConfig verificationConfig;
    @InjectMocks
    private ResendVerificationService resendVerificationService;

    @Captor
    private ArgumentCaptor<String> otpCaptor;

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String HASHED_OTP = "$argon2id$hashedOtp";

    private Account buildPendingAccount() {

        return Account.newPendingVerification(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER
        );
    }

    @Test
    void resend_pendingAccount_generatesNewOtpAndSendsEmail() {

        var email = new Email(EMAIL);
        var account = buildPendingAccount();
        var otpTtl = Duration.ofSeconds(900);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpHasher.hash(any(String.class))).willReturn(HASHED_OTP);
        given(verificationConfig.otpTtl()).willReturn(otpTtl);

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpHasher).should().hash(otpCaptor.capture());
        then(otpRepository).should().save(account.getId(), HASHED_OTP, otpTtl);
        then(emailVerificationPort).should().sendVerificationCode(eq(email), eq(otpCaptor.getValue()));
    }

    @Test
    void resend_unknownEmail_doesNothingSilently() {

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.empty());

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpRepository).should(never()).save(any(), any(), any());
        then(emailVerificationPort).should(never()).sendVerificationCode(any(), any());
    }

    @Test
    void resend_accountNotPending_doesNothingSilently() {

        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.of(activeAccount));

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpRepository).should(never()).save(any(), any(), any());
        then(emailVerificationPort).should(never()).sendVerificationCode(any(), any());
    }
}
