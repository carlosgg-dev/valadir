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
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.PlainOtp;
import com.valadir.domain.model.Role;
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

    private static final PlainOtp PLAIN_OTP = PlainOtp.generate();
    private static final HashedOtp HASHED_OTP = new HashedOtp("$argon2id$hashedOtp");
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(10);

    @Test
    void verify_validOtp_deletesOtpAndIssuesVerificationToken() {

        var email = Email.from("bruce.wayne@email.com");
        var account = buildAccount(email.value());
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(true);
        given(passwordResetConfig.verificationTokenTtl()).willReturn(VERIFICATION_TTL);

        PasswordResetOtpVerificationResult result = service.verify(command);

        assertThat(result.verificationToken()).isNotBlank();
        then(otpRepository).should().delete(account.getId());
        then(passwordResetVerificationTokenRepository).should().save(result.verificationToken(), account.getId(), VERIFICATION_TTL);
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
        then(otpRepository).should(never()).delete(any());
        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
    }

    @Test
    void verify_otpNotFound_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var account = buildAccount(email.value());
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.verify(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_OTP);

        then(otpRepository).should(never()).delete(any());
        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
    }

    @Test
    void verify_wrongOtp_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var account = buildAccount(email.value());
        var command = new VerifyPasswordResetOtpCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.verify(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_OTP);

        then(otpRepository).should(never()).delete(any());
        then(passwordResetVerificationTokenRepository).should(never()).save(any(), any(), any());
    }

    private Account buildAccount(String email) {

        return Account.reconstitute(
            AccountId.generate(),
            Email.from(email),
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );
    }
}
