package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.domain.model.UserProfileData;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class RegisterService implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterService.class);

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordSecurityService passwordSecurityService;
    private final RegisterPersistence registerPersistence;
    private final OtpVerificationSender otpVerificationSender;

    public RegisterService(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        OtpVerificationSender otpVerificationSender
    ) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.passwordSecurityService = passwordSecurityService;
        this.registerPersistence = registerPersistence;
        this.otpVerificationSender = otpVerificationSender;
    }

    @Override
    public void register(RegisterCommand command) {

        var email = new Email(command.email());
        var rawPassword = new RawPassword(command.password());
        var fullName = new FullName(command.fullName());
        var givenName = new GivenName(command.givenName());

        var existingAccountId = accountRepository.findByEmail(email)
            .map(this::resolveExistingAccountId);

        var profileData = new UserProfileData(fullName, givenName);
        passwordSecurityService.validatePassword(rawPassword, email, profileData);

        var accountId = AccountId.generate();
        MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());

        var hashedPassword = passwordHasher.hash(rawPassword);
        var account = Account.newPendingVerification(accountId, email, hashedPassword, Role.USER);
        var user = User.newProfile(UserId.generate(), accountId, fullName, givenName);

        if (existingAccountId.isPresent()) {
            log.info("Re-registration: replacing stale PENDING_VERIFICATION");
            registerPersistence.replacePendingAndSave(existingAccountId.get(), account, user);
        } else {
            registerPersistence.save(account, user);
        }

        otpVerificationSender.send(accountId, email);

        log.info("Registration successful, pending email verification");
    }

    private AccountId resolveExistingAccountId(Account existing) {

        return switch (existing.getStatus()) {
            case PENDING_VERIFICATION -> existing.getId();
            case ACTIVE -> {
                log.warn("Registration attempt with already-registered email, existingAccountId={}", existing.getId().value());
                throw new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        };
    }
}
