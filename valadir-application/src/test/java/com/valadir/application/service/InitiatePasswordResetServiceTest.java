package com.valadir.application.service;

import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.test.mother.AccountMother;
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
class InitiatePasswordResetServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OtpHasher otpHasher;

    @Mock
    private PasswordResetOtpSender passwordResetOtpSender;

    @InjectMocks
    private InitiatePasswordResetService service;

    @Test
    void initiate_existingActiveAccount_sendsResetCode() {

        var email = Email.from("bruce.wayne@email.com");
        var activeAccount = AccountMother.active().withEmail(email).build();
        var command = new InitiatePasswordResetCommand(email.value());

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(activeAccount));

        service.initiate(command);

        then(passwordResetOtpSender).should().send(activeAccount.getId(), email);
        then(otpHasher).should(never()).decoyMatch();
    }

    @Test
    void initiate_accountNotFound_decoyMatchAndReturnsSilently() {

        var email = Email.from("bruce.wayne@email.com");
        var command = new InitiatePasswordResetCommand(email.value());

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        service.initiate(command);

        then(otpHasher).should().decoyMatch();
        then(passwordResetOtpSender).should(never()).send(any(), any());
    }

    @Test
    void initiate_pendingActivationAccount_decoyMatchAndReturnsSilently() {

        var email = Email.from("bruce.wayne@email.com");
        var pendingAccount = AccountMother.pendingActivation().withEmail(email).build();
        var command = new InitiatePasswordResetCommand(email.value());

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));

        service.initiate(command);

        then(otpHasher).should().decoyMatch();
        then(passwordResetOtpSender).should(never()).send(any(), any());
    }

    @Test
    void initiate_invalidEmail_translatesDomainExceptionPreservingErrorCode() {

        var command = new InitiatePasswordResetCommand("not-an-email");

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.initiate(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FIELD);

        then(accountRepository).should(never()).findByEmail(any());
    }
}
