package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserProfileData;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
    private PasswordSecurityService passwordSecurityService;
    @Mock
    private RegisterPersistence registerPersistence;
    @Mock
    private OtpVerificationSender otpVerificationSender;
    @InjectMocks
    private RegisterService registerService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void register_emailNotExist_persistsDataAndSendsOtp() {

        var emailValue = "bruce.wayne@emailValue.com";
        var email = new Email(emailValue);
        var rawPasswordValue = "SecureP@ss123";
        var rawPassword = new RawPassword(rawPasswordValue);
        var hashedPassword = new HashedPassword("$2a$12$hashed");
        var fullNameValue = "Bruce Wayne";
        var givenNameValue = "Bruce";
        var fullName = new FullName(fullNameValue);
        var givenName = new GivenName(givenNameValue);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordHasher.hash(rawPassword)).willReturn(hashedPassword);

        registerService.register(new RegisterCommand(emailValue, rawPasswordValue, fullNameValue, givenNameValue));

        then(passwordSecurityService).should().validatePassword(rawPassword, email, new UserProfileData(fullName, givenName));
        then(registerPersistence).should().save(accountCaptor.capture(), userCaptor.capture());
        then(registerPersistence).should(never()).replacePendingAndSave(any(), any(), any());

        var savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getEmail()).isEqualTo(email);
        assertThat(savedAccount.getPassword()).isEqualTo(hashedPassword);
        assertThat(savedAccount.getRole()).isEqualTo(Role.USER);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getGivenName()).isEqualTo(givenName);
        assertThat(savedUser.getAccountId()).isEqualTo(savedAccount.getId());

        then(otpVerificationSender).should().send(savedAccount.getId(), email);
    }

    @Test
    void register_activeEmailExists_throwsApplicationException() {

        var emailValue = "bruce.wayne@email.com";
        var email = new Email(emailValue);
        var existing = Account.reconstitute(
            AccountId.generate(),
            email,
            new HashedPassword("$argon2id$hashed"),
            Role.USER,
            AccountStatus.ACTIVE
        );

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(existing));

        RegisterCommand command = new RegisterCommand(emailValue, "SecureP@ss123", "Bruce Wayne", "Bruce");
        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> registerService.register(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        then(registerPersistence).should(never()).replacePendingAndSave(any(), any(), any());
        then(registerPersistence).should(never()).save(any(), any());
        then(otpVerificationSender).should(never()).send(any(), any());
    }

    @Test
    void register_pendingEmailExists_replacesPending() {

        var emailValue = "bruce.wayne@email.com";
        var email = new Email(emailValue);
        var staleAccountId = AccountId.generate();
        var staleAccount = Account.newPendingVerification(
            staleAccountId,
            email,
            new HashedPassword("$argon2id$old"),
            Role.USER
        );
        var rawPasswordValue = "SecureP@ss123";
        var hashedPassword = new HashedPassword("$argon2id$new");

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(staleAccount));
        given(passwordHasher.hash(new RawPassword(rawPasswordValue))).willReturn(hashedPassword);

        registerService.register(new RegisterCommand(emailValue, rawPasswordValue, "Bruce Wayne", "Bruce"));

        then(registerPersistence).should().replacePendingAndSave(eq(staleAccountId), accountCaptor.capture(), userCaptor.capture());
        then(registerPersistence).should(never()).save(any(), any());

        var newAccount = accountCaptor.getValue();
        var newUser = userCaptor.getValue();
        assertThat(newAccount.getId()).isNotEqualTo(staleAccountId);
        assertThat(newAccount.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(newUser.getAccountId()).isEqualTo(newAccount.getId());
        then(otpVerificationSender).should().send(newAccount.getId(), email);
    }
}
