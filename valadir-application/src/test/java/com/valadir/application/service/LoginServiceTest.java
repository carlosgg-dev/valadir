package com.valadir.application.service;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.AccountLockedException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
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

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private LoginService service;

    @Test
    void login_validCredentials_returnsTokens() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var accessToken = "access-token";
        var refreshToken = "refresh-token";

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(true);
        given(authTokenIssuer.issue(EXISTING_ACCOUNT.getId(), EXISTING_ACCOUNT.getRole())).willReturn(new AuthTokenResult(accessToken, refreshToken));

        AuthTokenResult result = service.login(new LoginCommand(email, password));

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        then(refreshTokenRepository).should().save(refreshToken, EXISTING_ACCOUNT.getId());
    }

    @Test
    void login_emailNotFoundGuardsTiming_throwsApplicationException() {

        var email = Email.from("unknown@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var command = new LoginCommand(email, password);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(passwordHasher).should().guardTiming(password);
        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_wrongPassword_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("WrongP@ss123");
        var command = new LoginCommand(email, password);

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(authTokenIssuer).should(never()).issue(any(), any());
    }

    @Test
    void login_accountPendingActivation_throwsApplicationException() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var command = new LoginCommand(email, password);
        var pendingAccount = Account.newPendingActivation(
            AccountId.generate(),
            email,
            new HashedPassword("$2a$12$hashed"),
            Role.USER
        );

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        given(passwordHasher.matches(password, pendingAccount.getPassword())).willReturn(true);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PENDING_ACTIVATION);

        then(authTokenIssuer).should(never()).issue(any(), any());
        then(loginAttemptRepository).should(never()).clearAttempts(any());
    }

    @Test
    void login_withActiveLockout_throwsAccountLockedException() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var remainingLockout = Duration.ofSeconds(30);
        var command = new LoginCommand(email, password);

        given(loginAttemptRepository.findActiveLockout(email)).willReturn(Optional.of(remainingLockout));

        assertThatExceptionOfType(AccountLockedException.class)
            .isThrownBy(() -> service.login(command))
            .satisfies(e -> assertThat(e.lockout()).isEqualTo(Duration.ofSeconds(30)));

        then(accountRepository).should(never()).findByEmail(any());
    }

    @Test
    void login_wrongPassword_recordsFailedAttempt() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var command = new LoginCommand(email, password);

        given(loginAttemptRepository.findActiveLockout(email)).willReturn(Optional.empty());
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(false);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(loginAttemptRepository).should().recordFailedAttempt(email);
        then(loginAttemptRepository).should(never()).clearAttempts(any());
    }

    @Test
    void login_emailNotFound_doesNotRecordFailedAttempt() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var command = new LoginCommand(email, password);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);

        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
    }

    @Test
    void login_accountPendingActivation_doesNotRecordFailedAttempt() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");
        var command = new LoginCommand(email, password);
        var pendingAccount = Account.newPendingActivation(
            AccountId.generate(),
            email,
            new HashedPassword("$2a$12$hashed"),
            Role.USER
        );

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(pendingAccount));
        given(passwordHasher.matches(password, pendingAccount.getPassword())).willReturn(true);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.login(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_PENDING_ACTIVATION);

        then(loginAttemptRepository).should(never()).recordFailedAttempt(any());
    }

    @Test
    void login_validCredentials_clearsAttempts() {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("SecureP@ss123");

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(EXISTING_ACCOUNT));
        given(passwordHasher.matches(password, EXISTING_ACCOUNT.getPassword())).willReturn(true);
        given(authTokenIssuer.issue(EXISTING_ACCOUNT.getId(), EXISTING_ACCOUNT.getRole()))
            .willReturn(new AuthTokenResult("access-token", "refresh-token"));

        service.login(new LoginCommand(email, password));

        then(loginAttemptRepository).should().clearAttempts(email);
    }

    private static final Account EXISTING_ACCOUNT = Account.reconstitute(
        AccountId.generate(),
        Email.from("bruce.wayne@email.com"),
        new HashedPassword("$2a$12$hashed"),
        Role.USER,
        AccountStatus.ACTIVE
    );
}
