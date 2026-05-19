package com.valadir.application.service;

import com.valadir.application.command.ResendAccountActivationCodeCommand;
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
class ResendAccountActivationCodeServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountActivationOtpSender accountActivationOtpSender;

    @InjectMocks
    private ResendAccountActivationCodeService resendAccountActivationCodeService;

    private static final String EMAIL = "bruce.wayne@email.com";

    @Test
    void resend_pendingActivationAccount_sendsActivationCode() {

        var email = new Email(EMAIL);
        var account = buildPendingActivationAccount();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(EMAIL));

        then(accountActivationOtpSender).should().send(account.getId(), email);
    }

    @Test
    void resend_unknownEmail_doesNothingSilently() {

        given(accountRepository.findByEmail(new Email(EMAIL))).willReturn(Optional.empty());

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(EMAIL));

        then(accountActivationOtpSender).should(never()).send(any(), any());
    }

    @Test
    void resend_accountNotPending_doesNothingSilently() {

        var email = new Email(EMAIL);
        var activeAccount = Account.reconstitute(
            AccountId.generate(),
            email,
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(EMAIL));

        then(accountActivationOtpSender).should(never()).send(any(), any());
    }

    private Account buildPendingActivationAccount() {

        return Account.newPendingActivation(
            AccountId.generate(),
            new Email(EMAIL),
            new HashedPassword("$argon2id$hashed"),
            Role.USER
        );
    }
}
