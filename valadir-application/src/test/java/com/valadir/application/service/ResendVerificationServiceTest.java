package com.valadir.application.service;

import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ResendVerificationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OtpVerificationSender otpVerificationSender;
    @InjectMocks
    private ResendVerificationService resendVerificationService;

    private static final String EMAIL = "bruce.wayne@email.com";

    private Account buildPendingAccount() {

        return Account.newPendingVerification(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER
        );
    }

    @Test
    void resend_pendingAccount_sendsVerificationCode() {

        var email = new Email(EMAIL);
        var account = buildPendingAccount();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpVerificationSender).should().send(account.getId(), email);
    }

    @Test
    void resend_unknownEmail_doesNothingSilently() {

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.empty());

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpVerificationSender).should(never()).send(any(), any());
    }

    @Test
    void resend_accountNotPending_doesNothingSilently() {

        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.of(activeAccount));

        resendVerificationService.resend(new ResendVerificationCommand(EMAIL));

        then(otpVerificationSender).should(never()).send(any(), any());
    }
}
