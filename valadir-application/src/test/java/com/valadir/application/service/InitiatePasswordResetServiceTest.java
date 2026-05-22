package com.valadir.application.service;

import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
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

    private static final Email EMAIL = new Email("bruce.wayne@email.com");
    private static final HashedOtp HASHED_OTP = new HashedOtp("$argon2id$hashedOtp");
    private static final HashedPassword HASHED_PASSWORD = new HashedPassword("$argon2id$hashed");
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Test
    void initiate_existingActiveAccount_sendsResetCode() {

        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            EMAIL,
            HASHED_PASSWORD,
            Role.USER,
            AccountStatus.ACTIVE
        );

        var command = new InitiatePasswordResetCommand(EMAIL.value());

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.of(activeAccount));
        given(otpHasher.hash(any())).willReturn(HASHED_OTP);
        given(passwordResetConfig.otpTtl()).willReturn(OTP_TTL);

        service.initiate(command);

        then(otpHasher).should().hash(plainOtpCaptor.capture());
        then(otpRepository).should().save(activeAccount.getId(), HASHED_OTP, OTP_TTL);
        then(passwordResetNotifier).should().sendResetCode(EMAIL, plainOtpCaptor.getValue());
    }

    @Test
    void initiate_accountNotFound_guardTimingAndReturnsSilently() {

        var command = new InitiatePasswordResetCommand(EMAIL.value());

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        service.initiate(command);

        then(otpHasher).should().guardTiming();
        then(otpHasher).should(never()).hash(any());
        then(otpRepository).should(never()).save(any(), any(), any());
        then(passwordResetNotifier).should(never()).sendResetCode(any(), any());
    }

    @Test
    void initiate_pendingActivationAccount_guardTimingAndReturnsSilently() {

        var pendingAccount = Account.newPendingActivation(AccountId.generate(), EMAIL, HASHED_PASSWORD, Role.USER);
        var command = new InitiatePasswordResetCommand(EMAIL.value());

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.of(pendingAccount));
        service.initiate(command);

        then(otpHasher).should().guardTiming();
        then(otpHasher).should(never()).hash(any());
        then(otpRepository).should(never()).save(any(), any(), any());
        then(passwordResetNotifier).should(never()).sendResetCode(any(), any());
    }
}
