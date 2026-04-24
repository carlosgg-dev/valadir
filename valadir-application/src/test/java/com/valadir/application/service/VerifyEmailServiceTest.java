package com.valadir.application.service;

import com.valadir.application.command.VerifyEmailCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.common.error.ErrorCode;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VerifyEmailServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OtpStore otpStore;
    @Mock
    private OtpHasher otpHasher;
    @InjectMocks
    private VerifyEmailService service;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String PLAIN_CODE = "123456";
    private static final String HASHED_CODE = "$argon2id$hashedOtp";

    private Account buildPendingAccount() {

        return Account.newPendingVerification(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER
        );
    }

    @Test
    void verify_validCode_activatesAccountAndDeletesToken() {

        var pendingAccount = buildPendingAccount();
        var command = new VerifyEmailCommand(EMAIL, PLAIN_CODE);

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.of(pendingAccount));
        given(otpStore.find(pendingAccount.getId())).willReturn(Optional.of(HASHED_CODE));
        given(otpHasher.matches(PLAIN_CODE, HASHED_CODE)).willReturn(true);

        service.verify(command);

        then(accountRepository).should().save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().isActive()).isTrue();
        then(otpStore).should().delete(pendingAccount.getId());
    }

    @Test
    void verify_accountNotFound_throwsInvalidVerificationOtp() {

        var command = new VerifyEmailCommand(EMAIL, PLAIN_CODE);

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_OTP);

        then(otpStore).should(never()).find(any());
        then(accountRepository).should(never()).save(any());
        then(otpStore).should(never()).delete(any());
    }

    @Test
    void verify_accountAlreadyActive_throwsInvalidVerificationOtp() {

        var email = new Email(EMAIL);
        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            email,
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        var command = new VerifyEmailCommand(EMAIL, PLAIN_CODE);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> service.verify(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_OTP);

        then(otpStore).should(never()).find(any());
        then(accountRepository).should(never()).save(any());
        then(otpStore).should(never()).delete(any());
    }

    @Test
    void verify_codeNotFound_throwsInvalidVerificationOtp() {

        var pendingAccount = buildPendingAccount();
        var command = new VerifyEmailCommand(EMAIL, PLAIN_CODE);

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.of(pendingAccount));
        given(otpStore.find(pendingAccount.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_OTP);

        then(accountRepository).should(never()).save(any());
        then(otpStore).should(never()).delete(any());
    }

    @Test
    void verify_wrongCode_throwsInvalidVerificationOtp() {

        var account = buildPendingAccount();
        var command = new VerifyEmailCommand(EMAIL, PLAIN_CODE);

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.of(account));
        given(otpStore.find(account.getId())).willReturn(Optional.of(HASHED_CODE));
        given(otpHasher.matches(PLAIN_CODE, HASHED_CODE)).willReturn(false);

        assertThatThrownBy(() -> service.verify(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_OTP);

        then(accountRepository).should(never()).save(any());
        then(otpStore).should(never()).delete(any());
    }
}
