package com.valadir.application.service;

import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.OtpMother;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InitiatePasswordResetServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private PasswordResetNotifier passwordResetNotifier;

    @Mock
    private PasswordResetConfig passwordResetConfig;

    @InjectMocks
    private InitiatePasswordResetService service;

    @Captor
    private ArgumentCaptor<PlainOtp> plainOtpCaptor;

    private static final HashedOtp HASHED_OTP = OtpMother.hashed();
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Test
    void initiate_existingActiveAccount_sendsResetCode() {

        var email = Email.from("bruce.wayne@email.com");
        var activeAccount = AccountMother.active().withEmail(email).build();
        var command = new InitiatePasswordResetCommand(email);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));
        given(otpHasher.hash(any())).willReturn(HASHED_OTP);
        given(passwordResetConfig.otpTtl()).willReturn(OTP_TTL);

        service.initiate(command);

        then(otpHasher).should().hash(plainOtpCaptor.capture());
        then(otpRepository).should().save(activeAccount.getId(), HASHED_OTP, OTP_TTL);
        then(passwordResetNotifier).should().sendResetCode(email, plainOtpCaptor.getValue());
    }

    @Test
    void initiate_accountNotFound_guardTimingAndReturnsSilently() {

        var email = Email.from("bruce.wayne@email.com");
        var command = new InitiatePasswordResetCommand(email);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        service.initiate(command);

        then(otpHasher).should().guardTiming();
        then(otpHasher).should(never()).hash(any());
        then(otpRepository).should(never()).save(any(), any(), any());
        then(passwordResetNotifier).should(never()).sendResetCode(any(), any());
    }

    @Test
    void initiate_pendingActivationAccount_guardTimingAndReturnsSilently() {

        var email = Email.from("bruce.wayne@email.com");
        var pendingAccount = AccountMother.pendingActivation().withEmail(email).build();
        var command = new InitiatePasswordResetCommand(email);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        service.initiate(command);

        then(otpHasher).should().guardTiming();
        then(otpHasher).should(never()).hash(any());
        then(otpRepository).should(never()).save(any(), any(), any());
        then(passwordResetNotifier).should(never()).sendResetCode(any(), any());
    }
}
