package com.valadir.application.service;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.OtpMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AccountActivationOtpSenderServiceTest {

    @Mock
    private AccountActivationNotifier accountActivationNotifier;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private AccountActivationConfig accountActivationConfig;

    @InjectMocks
    private AccountActivationOtpSenderService accountActivationOtpSenderService;

    @Captor
    private ArgumentCaptor<PlainOtp> plainOtpCaptor;

    @Test
    void send_hashesOtpPersistsAndSendsEmail() {

        var accountId = AccountId.generate();
        var email = Email.from("bruce.wayne@email.com");
        var hashedOtp = OtpMother.hashed();
        var otpTtl = Duration.ofSeconds(900);

        given(otpHasher.hash(any(PlainOtp.class))).willReturn(hashedOtp);
        given(accountActivationConfig.otpTtl()).willReturn(otpTtl);

        accountActivationOtpSenderService.send(accountId, email);

        then(otpHasher).should().hash(plainOtpCaptor.capture());
        var capturedOtp = plainOtpCaptor.getValue();

        then(otpRepository).should().save(accountId, hashedOtp, otpTtl);
        then(accountActivationNotifier).should().sendActivationCode(email, capturedOtp);
    }
}
