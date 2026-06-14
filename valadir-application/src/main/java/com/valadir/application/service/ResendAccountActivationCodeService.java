package com.valadir.application.service;

import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ResendAccountActivationCodeService implements ResendAccountActivationCodeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendAccountActivationCodeService.class);

    private final AccountRepository accountRepository;
    private final AccountActivationOtpSender accountActivationOtpSender;

    public ResendAccountActivationCodeService(AccountRepository accountRepository, AccountActivationOtpSender accountActivationOtpSender) {

        this.accountRepository = accountRepository;
        this.accountActivationOtpSender = accountActivationOtpSender;
    }

    @Override
    public void resend(ResendAccountActivationCodeCommand command) {

        var email = Email.from(command.email());

        accountRepository.findByEmail(email).ifPresentOrElse(
            account -> {
                MDC.put(MdcKeys.ACCOUNT_ID, account.getId().value().toString());
                if (!account.isPendingActivation()) {
                    log.warn("Resend account activation OTP attempted for already active account");
                    return;
                }
                accountActivationOtpSender.send(account.getId(), email);
                log.info("Account activation OTP resent");
            },
            () -> log.warn("Resend account activation OTP attempted for unknown email")
        );
    }
}
