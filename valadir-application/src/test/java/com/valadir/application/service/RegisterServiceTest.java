package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    @SuppressWarnings("unused") // Required for constructor injection — passed to Account.createWithProfileSafety()
    private PasswordSecurityService passwordSecurityService;
    @Mock
    private RegisterPersistence registerPersistence;
    @Mock
    private AuthTokenIssuer authTokenIssuer;
    @InjectMocks
    private RegisterService service;

    @Test
    void shouldRegisterUser_WhenDataIsValid() {

        var email = "bruce.wayne@email.com";
        var password = "SecureP@ss123";
        var accessToken = "access-token";
        var refreshToken = "refresh-token";

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.empty());
        given(passwordHasher.hash(new RawPassword(password))).willReturn(new HashedPassword("$2a$12$hashed"));
        given(authTokenIssuer.issue(any(AccountId.class), eq(Role.USER))).willReturn(new AuthTokenResult(accessToken, refreshToken));

        var result = service.register(new RegisterCommand(email, password, "Bruce Wayne", "Bruce"));

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        then(registerPersistence).should().save(any(Account.class), any(User.class));
    }

    @Test
    void shouldThrowException_WhenEmailAlreadyExists() {

        String email = "bruce.wayne@email.com";
        String password = "SecureP@ss123";

        var existing = Account.reconstitute(
            AccountId.generate(),
            new Email(email),
            new HashedPassword("$2a$12$hashed"),
            Role.USER
        );

        var command = new RegisterCommand(email, password, "Bruce Wayne", "Bruce");

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        then(registerPersistence).should(never()).save(any(), any());
        then(authTokenIssuer).should(never()).issue(any(), any());
    }
}
