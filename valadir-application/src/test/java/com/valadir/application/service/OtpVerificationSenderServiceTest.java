package com.valadir.application.service;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OtpVerificationSenderServiceTest {

    @Mock
    private EmailVerificationPort emailVerificationPort;
    @Mock
    private OtpStore otpStore;
    @Mock
    private OtpHasher otpHasher;
    @Mock
    private VerificationConfig verificationConfig;
    @InjectMocks
    private OtpVerificationSenderService otpVerificationSenderService;

    @Captor
    private ArgumentCaptor<String> plainCodeCaptor;

    @Test
    void send_hashesOtpPersistsAndSendsEmail() {

        var accountId = AccountId.generate();
        var email = new Email("bruce.wayne@email.com");
        var hashedOtp = "$argon2id$hashedOtp";
        var otpTtl = Duration.ofSeconds(900);

        given(otpHasher.hash(anyString())).willReturn(hashedOtp);
        given(verificationConfig.otpTtl()).willReturn(otpTtl);

        otpVerificationSenderService.send(accountId, email);

        then(otpHasher).should().hash(plainCodeCaptor.capture());
        var capturedCode = plainCodeCaptor.getValue();

        then(otpStore).should().save(accountId, hashedOtp, otpTtl);
        then(emailVerificationPort).should().sendVerificationCode(email, capturedCode);
    }
}
