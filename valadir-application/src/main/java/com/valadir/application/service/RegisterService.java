package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.result.AuthTokenResult;
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
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;

    public RegisterService(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenStore refreshTokenStore
    ) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.passwordSecurityService = passwordSecurityService;
        this.registerPersistence = registerPersistence;
        this.authTokenIssuer = authTokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
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
        MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());
        var hashedPassword = passwordHasher.hash(rawPassword);
        var profileData = new UserProfileData(fullName, givenName);
        passwordSecurityService.validatePassword(rawPassword, email, profileData);

        Account account = Account.from(accountId, email, hashedPassword, Role.USER);
        User user = User.newProfile(UserId.generate(), accountId, fullName, givenName);
        registerPersistence.save(account, user);

        AuthTokenResult tokens = authTokenIssuer.issue(accountId, Role.USER);
        refreshTokenStore.save(tokens.refreshToken(), accountId);
        log.info("Registration successful");
        return tokens;
    }
}
