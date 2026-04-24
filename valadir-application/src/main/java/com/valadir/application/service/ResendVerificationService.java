package com.valadir.application.service;

import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendVerificationService implements ResendVerificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendVerificationService.class);

    private final AccountRepository accountRepository;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;
    private final EmailVerificationPort emailVerificationPort;
    private final VerificationConfig verificationConfig;

    public ResendVerificationService(
        AccountRepository accountRepository,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        EmailVerificationPort emailVerificationPort,
        VerificationConfig verificationConfig
    ) {

        this.accountRepository = accountRepository;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
        this.emailVerificationPort = emailVerificationPort;
        this.verificationConfig = verificationConfig;
    }

    @Override
    public void resend(ResendVerificationCommand command) {

        var email = new Email(command.email());

        accountRepository.findByEmail(email).ifPresentOrElse(
            account -> {
                if (account.getStatus() != AccountStatus.PENDING_VERIFICATION) {
                    log.warn("Resend verification attempted for non-pending account [account={}]", account.getId().value());
                    return;
                }
                var plainCode = OtpGenerator.generate();
                otpRepository.save(account.getId(), otpHasher.hash(plainCode), verificationConfig.otpTtl());
                emailVerificationPort.sendVerificationCode(email, plainCode);
                log.info("Verification code resent [account={}]", account.getId().value());
            },
            () -> log.warn("Resend verification attempted for unknown email")
        );
    }
}
