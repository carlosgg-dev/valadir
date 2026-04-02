package com.valadir.application.service;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private AuthTokenIssuer authTokenIssuer;
    @InjectMocks
    private LoginService service;

    private static final Account EXISTING_ACCOUNT = Account.reconstitute(
        AccountId.generate(),
        new Email("bruce.wayne@email.com"),
        new HashedPassword("$2a$12$hashed"),
        Role.USER
    );

    @Test
    void shouldLogin_WhenCredentialsAreValid() {

        var email = "bruce.wayne@email.com";
        var password = "SecureP@ss123";
        var accessToken = "access-token";
        var refreshToken = "refresh-token";

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(new RawPassword(password), EXISTING_ACCOUNT.getPassword())).willReturn(true);
        given(authTokenIssuer.issue(EXISTING_ACCOUNT.getId(), EXISTING_ACCOUNT.getRole())).willReturn(new AuthTokenResult(accessToken, refreshToken));

        var result = service.login(new LoginCommand(email, password));

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void shouldThrowException_WhenEmailNotFound() {

        var email = "unknown@email.com";
        var password = "SecureP@ss123";
        var command = new LoginCommand(email, password);

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void shouldThrowException_WhenPasswordIsWrong() {

        var email = "bruce.wayne@email.com";
        var password = "WrongP@ss123";
        var command = new LoginCommand(email, password);

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(new RawPassword(password), EXISTING_ACCOUNT.getPassword())).willReturn(false);

        assertThatThrownBy(() -> service.login(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(authTokenIssuer).should(never()).issue(any(), any());
    }
}
