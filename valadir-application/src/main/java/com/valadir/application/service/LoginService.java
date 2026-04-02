package com.valadir.application.service;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;

public class LoginService implements LoginUseCase {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final AuthTokenIssuer authTokenIssuer;

    public LoginService(AccountRepository accountRepository, PasswordHasher passwordHasher, AuthTokenIssuer authTokenIssuer) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.authTokenIssuer = authTokenIssuer;
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {

        var email = new Email(command.email());
        var rawPassword = new RawPassword(command.password());

        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR));

        if (!passwordHasher.matches(rawPassword, account.getPassword())) {
            throw new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
        }

        return authTokenIssuer.issue(account.getId(), account.getRole());
    }
}
