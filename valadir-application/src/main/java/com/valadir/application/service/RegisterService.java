package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.out.PasswordHasher;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.domain.service.PasswordSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class RegisterService implements RegisterUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterService.class);

    private final EmailHolderResolver emailHolderResolver;
    private final PasswordHasher passwordHasher;
    private final PasswordSecurityService passwordSecurityService;
    private final RegisterPersistence registerPersistence;
    private final AccountActivationOtpSender accountActivationOtpSender;

    public RegisterService(
        EmailHolderResolver emailHolderResolver,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        AccountActivationOtpSender accountActivationOtpSender
    ) {

        this.emailHolderResolver = emailHolderResolver;
        this.passwordHasher = passwordHasher;
        this.passwordSecurityService = passwordSecurityService;
        this.registerPersistence = registerPersistence;
        this.accountActivationOtpSender = accountActivationOtpSender;
    }

    @Override
    public void register(RegisterCommand command) {

        try {
            var email = Email.from(command.email());
            var rawPassword = RawPassword.newPassword(command.password());
            var fullName = FullName.from(command.fullName());
            var givenName = GivenName.from(command.givenName());
            var language = Language.forTag(command.language());

            var existingAccountId = emailHolderResolver.replaceableHolderFor(email);

            var accountId = AccountId.generate();
            var user = User.newProfile(UserId.generate(), accountId, fullName, givenName);
            passwordSecurityService.validatePassword(rawPassword, email, user);

            MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());

            var hashedPassword = passwordHasher.hash(rawPassword);
            var account = Account.newPendingActivation(accountId, email, hashedPassword, Role.USER, language);

            if (existingAccountId.isPresent()) {
                log.info("Re-registration: replacing an account pending activation");
                registerPersistence.replace(existingAccountId.get(), account, user);
            } else {
                registerPersistence.save(account, user);
            }

            accountActivationOtpSender.send(account);

            log.info("Registration successful, pending account activation");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
