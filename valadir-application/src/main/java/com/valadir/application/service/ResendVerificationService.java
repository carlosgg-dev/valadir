package com.valadir.application.service;

import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendVerificationService implements ResendVerificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendVerificationService.class);

    private final AccountRepository accountRepository;
    private final OtpVerificationSender otpVerificationSender;

    public ResendVerificationService(AccountRepository accountRepository, OtpVerificationSender otpVerificationSender) {

        this.accountRepository = accountRepository;
        this.otpVerificationSender = otpVerificationSender;
    }

    @Override
    public void resend(ResendVerificationCommand command) {

        var email = new Email(command.email());

        accountRepository.findByEmail(email).ifPresentOrElse(
            account -> {
                if (account.getStatus() != AccountStatus.PENDING_VERIFICATION) {
                    log.warn("Resend verification attempted for non-pending account, accountId={}", account.getId().value());
                    return;
                }
                otpVerificationSender.send(account.getId(), email);
                log.info("Verification code resent, accountId={}", account.getId().value());
            },
            () -> log.warn("Resend verification attempted for unknown email")
        );
    }
}
