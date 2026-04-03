package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.domain.model.UserProfileData;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;

public class RegisterService implements RegisterUseCase {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordSecurityService passwordSecurityService;
    private final RegisterPersistence registerPersistence;
    private final AuthTokenIssuer authTokenIssuer;

    public RegisterService(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        AuthTokenIssuer authTokenIssuer
    ) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.passwordSecurityService = passwordSecurityService;
        this.registerPersistence = registerPersistence;
        this.authTokenIssuer = authTokenIssuer;
    }

    @Override
    public AuthTokenResult register(RegisterCommand command) {

        var email = new Email(command.email());
        var rawPassword = new RawPassword(command.password());
        var fullName = new FullName(command.fullName());
        var givenName = new GivenName(command.givenName());

        if (accountRepository.findByEmail(email).isPresent()) {
            throw new ApplicationException("Email already exists", ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        var accountId = AccountId.generate();
        HashedPassword hashedPassword = passwordHasher.hash(rawPassword);
        var profileData = new UserProfileData(fullName, givenName);
        passwordSecurityService.validatePassword(rawPassword, email, profileData);

        Account account = Account.create(accountId, email, hashedPassword, Role.USER);
        User user = User.createNewProfile(UserId.generate(), accountId, fullName, givenName);
        registerPersistence.save(account, user);

        return authTokenIssuer.issue(accountId, Role.USER);
    }
}
