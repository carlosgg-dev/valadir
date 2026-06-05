package com.valadir.application.service;

import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.application.result.PasswordResetOtpVerificationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.OtpMother;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VerifyPasswordResetOtpServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository;

    @Mock
    private PasswordResetConfig passwordResetConfig;

    @InjectMocks
    private VerifyPasswordResetOtpService service;

    private static final PlainOtp PLAIN_OTP = OtpMother.plain();
    private static final HashedOtp HASHED_OTP = OtpMother.hashed();
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(10);

    @Test
    void verify_validOtp_deletesOtpAndIssuesVerificationToken() {

        var email = Email.from("bruce.wayne@email.com");
        var account = AccountMother.active().withEmail(email).build();
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(true);
        given(passwordResetConfig.verificationTokenTtl()).willReturn(VERIFICATION_TTL);

        PasswordResetOtpVerificationResult result = service.verify(command);

        assertThat(result.verificationToken()).isNotBlank();
        then(passwordResetVerificationTokenRepository).should().save(result.verificationToken(), account.getId(), VERIFICATION_TTL);
        then(otpRepository).should().delete(account.getId());
    }

    @Test
    void verify_emailNotFound_guardTimingAndThrowsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.verify(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_OTP);

        then(otpHasher).should().guardTiming();
        then(otpRepository).should(never()).find(any());
        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void verify_otpNotFound_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var account = AccountMother.active().withEmail(email).build();
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.verify(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_OTP);

        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void verify_wrongOtp_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var account = AccountMother.active().withEmail(email).build();
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.verify(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_OTP);

        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void verify_otpDeletionFails_verificationTokenAlreadySaved() {

        var email = Email.from("bruce.wayne@email.com");
        var account = AccountMother.active().withEmail(email).build();
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(true);
        given(passwordResetConfig.verificationTokenTtl()).willReturn(VERIFICATION_TTL);

        willThrow(InfrastructureException.class).given(otpRepository).delete(account.getId());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.verify(command));

        then(passwordResetVerificationTokenRepository).should().save(any(), eq(account.getId()), eq(VERIFICATION_TTL));
    }
}
