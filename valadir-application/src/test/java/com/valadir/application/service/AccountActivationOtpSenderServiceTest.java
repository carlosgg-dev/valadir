package com.valadir.application.service;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.Account;
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
class AccountActivationOtpSenderServiceTest {

    // Deliberately not the fallback language: a notifier told EN regardless would still pass.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;

    private static final Account ACCOUNT = AccountMother.pendingActivation()
        .withLanguage(ACCOUNT_LANGUAGE)
        .build();

    private static final Duration OTP_TTL = Duration.ofSeconds(900);

    @Mock
    private AccountActivationNotifier accountActivationNotifier;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @Captor
    private ArgumentCaptor<PlainOtp> plainOtpCaptor;

    private AccountActivationOtpSenderService accountActivationOtpSenderService;

    @BeforeEach
    void setUp() {

        accountActivationOtpSenderService = new AccountActivationOtpSenderService(
            accountActivationNotifier,
            otpRepository,
            otpHasher,
            new AccountActivationConfig(OTP_TTL)
        );
    }

    @Test
    void send_hashesOtpPersistsAndSendsEmail() {

        var hashedOtp = OtpMother.hashed();

        given(otpHasher.hash(any(PlainOtp.class))).willReturn(hashedOtp);

        accountActivationOtpSenderService.send(ACCOUNT);

        then(otpHasher).should().hash(plainOtpCaptor.capture());
        var capturedOtp = plainOtpCaptor.getValue();

        then(otpRepository).should().save(ACCOUNT.getId(), hashedOtp, OTP_TTL);
        var notification = new OtpNotification(ACCOUNT.getEmail(), capturedOtp, OTP_TTL, ACCOUNT_LANGUAGE);
        then(accountActivationNotifier).should().sendActivationCode(notification);
    }
}
