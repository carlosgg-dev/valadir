package com.valadir.application.service;

import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.domain.model.Email;
import com.valadir.test.mother.AccountMother;
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

    @Test
    void resend_pendingActivationAccount_sendsActivationCode() {

        var email = Email.from("bruce.wayne@email.com");
        var account = AccountMother.pendingActivation().withEmail(email).build();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(email));

        then(accountActivationOtpSender).should().send(account.getId(), email);
    }

    @Test
    void resend_unknownEmail_doesNothingSilently() {

        var email = Email.from("bruce.wayne@email.com");

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(email));

        then(accountActivationOtpSender).should(never()).send(any(), any());
    }

    @Test
    void resend_accountNotPending_doesNothingSilently() {

        var email = Email.from("bruce.wayne@email.com");
        var activeAccount = AccountMother.active().withEmail(email).build();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));

        resendAccountActivationCodeService.resend(new ResendAccountActivationCodeCommand(email));

        then(accountActivationOtpSender).should(never()).send(any(), any());
    }
}
