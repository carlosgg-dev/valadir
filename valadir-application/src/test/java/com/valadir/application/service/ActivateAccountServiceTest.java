package com.valadir.application.service;

import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ActivateAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private OtpHasher otpHasher;

    @InjectMocks
    private ActivateAccountService service;

    private static final PlainOtp PLAIN_OTP = PlainOtp.generate();
    private static final HashedOtp HASHED_OTP = new HashedOtp("$argon2id$hashedOtp");

    @Test
    void activate_validOtp_activatesAccountAndDeletesToken() {

        var email = Email.from("bruce.wayne@email.com");
        var pendingAccount = buildPendingActivationAccount(email.value());
        var command = new ActivateAccountCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        given(otpRepository.find(pendingAccount.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(true);

        service.activate(command);

        then(accountRepository).should().activate(pendingAccount.getId());
        then(otpRepository).should().delete(pendingAccount.getId());
    }

    @Test
    void activate_accountNotFound_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var command = new ActivateAccountCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.activate(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);

        then(otpRepository).should(never()).find(any());
        then(accountRepository).should(never()).activate(any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void activate_accountAlreadyActive_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            email,
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        var command = new ActivateAccountCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.activate(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);

        then(otpRepository).should(never()).find(any());
        then(accountRepository).should(never()).activate(any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void activate_otpNotFound_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var pendingAccount = buildPendingActivationAccount(email.value());
        var command = new ActivateAccountCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        given(otpRepository.find(pendingAccount.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.activate(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);

        then(accountRepository).should(never()).activate(any());
        then(otpRepository).should(never()).delete(any());
    }

    @Test
    void activate_wrongOtp_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var account = buildPendingActivationAccount(email.value());
        var command = new ActivateAccountCommand(email, PLAIN_OTP);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        given(otpRepository.find(account.getId())).willReturn(Optional.of(HASHED_OTP));
        given(otpHasher.matches(PLAIN_OTP, HASHED_OTP)).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.activate(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);

        then(accountRepository).should(never()).activate(any());
        then(otpRepository).should(never()).delete(any());
    }

    private Account buildPendingActivationAccount(String email) {

        return Account.newPendingActivation(
            AccountId.generate(),
            Email.from(email),
            new HashedPassword("$argon2id$hashed"),
            Role.USER
        );
    }
}
