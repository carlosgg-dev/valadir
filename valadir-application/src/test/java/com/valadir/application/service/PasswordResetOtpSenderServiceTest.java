package com.valadir.application.service;

import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.OtpMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PasswordResetOtpSenderServiceTest {

    // Deliberately not the fallback language: a notifier told EN regardless would still pass.
    private static final Language LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.active()
        .withEmail(Email.from("bruce.wayne@email.com"))
        .withLanguage(LANGUAGE)
        .build();

    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    // Never the OTP TTL: a service reading the wrong accessor would otherwise still pass.
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(5);

    @Mock
    private PasswordResetNotifier passwordResetNotifier;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @Captor
    private ArgumentCaptor<PlainOtp> plainOtpCaptor;

    private PasswordResetOtpSenderService passwordResetOtpSenderService;

    @BeforeEach
    void setUp() {

        passwordResetOtpSenderService = new PasswordResetOtpSenderService(
            passwordResetNotifier,
            otpRepository,
            otpHasher,
            new PasswordResetConfig(OTP_TTL, VERIFICATION_TOKEN_TTL)
        );
    }

    @Test
    void send_hashesOtpPersistsAndSendsEmail() {

        var hashedOtp = OtpMother.hashed();

        given(otpHasher.hash(any(PlainOtp.class))).willReturn(hashedOtp);

        passwordResetOtpSenderService.send(ACCOUNT);

        then(otpHasher).should().hash(plainOtpCaptor.capture());
        var capturedOtp = plainOtpCaptor.getValue();

        then(otpRepository).should().save(ACCOUNT.getId(), hashedOtp, OTP_TTL);
        // The same otpTtl on both: the expiry announced to the reader is the one actually enforced.
        var notification = new OtpNotification(ACCOUNT.getEmail(), capturedOtp, OTP_TTL, LANGUAGE);
        then(passwordResetNotifier).should().sendResetCode(notification);
    }
}
