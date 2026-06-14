package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
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
import static org.mockito.BDDMockito.willThrow;
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
    private AccountActivationOtpSender accountActivationOtpSender;

    @InjectMocks
    private RegisterService registerService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void register_emailNotExist_persistsDataAndSendsOtp() {

        var email = Email.from("bruce.wayne@emailValue.com");
        var rawPassword = PasswordMother.raw();
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");
        var hashedPassword = PasswordMother.hashed();

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordHasher.hash(rawPassword)).willReturn(hashedPassword);

        registerService.register(new RegisterCommand(email.value(), rawPassword.value(), fullName.value(), givenName.value()));

        then(registerPersistence).should().save(accountCaptor.capture(), userCaptor.capture());
        then(passwordSecurityService).should().validatePassword(rawPassword, email, userCaptor.getValue());
        then(registerPersistence).should(never()).replace(any(), any(), any());

        var savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getEmail()).isEqualTo(email);
        assertThat(savedAccount.getPassword()).isEqualTo(hashedPassword);
        assertThat(savedAccount.getRole()).isEqualTo(Role.USER);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getGivenName()).isEqualTo(givenName);
        assertThat(savedUser.getAccountId()).isEqualTo(savedAccount.getId());

        then(accountActivationOtpSender).should().send(savedAccount.getId(), email);
    }

    @Test
    void register_activeEmailExists_throwsApplicationException() {

        var email = Email.from("bruce.wayne@emailValue.com");
        var rawPassword = PasswordMother.raw();
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");
        var existing = AccountMother.active().withEmail(email).build();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(existing));

        RegisterCommand command = new RegisterCommand(email.value(), rawPassword.value(), fullName.value(), givenName.value());
        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> registerService.register(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        then(registerPersistence).should(never()).replace(any(), any(), any());
        then(registerPersistence).should(never()).save(any(), any());
        then(accountActivationOtpSender).should(never()).send(any(), any());
    }

    @Test
    void register_insecurePassword_translatesDomainExceptionPreservingErrorCode() {

        var email = Email.from("bruce.wayne@emailValue.com");
        var rawPassword = PasswordMother.raw();
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");
        var domainException = new DomainException("Password is insecure", ErrorCode.INSECURE_PASSWORD);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        willThrow(domainException).given(passwordSecurityService).validatePassword(eq(rawPassword), eq(email), any(User.class));

        RegisterCommand command = new RegisterCommand(email.value(), rawPassword.value(), fullName.value(), givenName.value());
        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> registerService.register(command))
            .withMessage(domainException.getMessage())
            .withCause(domainException)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INSECURE_PASSWORD);

        then(registerPersistence).should(never()).save(any(), any());
        then(accountActivationOtpSender).should(never()).send(any(), any());
    }

    @Test
    void register_pendingEmailExists_replacesPending() {

        var rawPassword = PasswordMother.raw();
        var oldHashedPassword = new HashedPassword("$argon2id$old");
        var newHashedPassword = new HashedPassword("$argon2id$new");

        var email = Email.from("bruce.wayne@emailValue.com");
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");

        var existingAccountId = AccountId.generate();
        var existingAccount = AccountMother.pendingActivation()
            .withId(existingAccountId)
            .withEmail(email)
            .withPassword(oldHashedPassword)
            .build();

        given(accountRepository.findByEmail(email)).willReturn(Optional.of(existingAccount));
        given(passwordHasher.hash(rawPassword)).willReturn(newHashedPassword);

        registerService.register(new RegisterCommand(email.value(), rawPassword.value(), fullName.value(), givenName.value()));

        then(registerPersistence).should().replace(eq(existingAccountId), accountCaptor.capture(), userCaptor.capture());
        then(registerPersistence).should(never()).save(any(), any());

        var newAccount = accountCaptor.getValue();
        var newUser = userCaptor.getValue();
        assertThat(newAccount.getId()).isNotEqualTo(existingAccountId);
        assertThat(newAccount.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(newUser.getAccountId()).isEqualTo(newAccount.getId());
        then(accountActivationOtpSender).should().send(newAccount.getId(), email);
    }
}
