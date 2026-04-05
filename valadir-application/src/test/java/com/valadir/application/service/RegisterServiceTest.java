package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.RegisterPersistence;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private RegisterService service;
    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void register_validData_savesAccountAndUser() {

        var email = "bruce.wayne@email.com";
        var password = "SecureP@ss123";
        var hashedPassword = new HashedPassword("$2a$12$hashed");
        var fullNameValue = "Bruce Wayne";
        var givenNameValue = "Bruce";
        var fullName = new FullName(fullNameValue);
        var givenName = new GivenName(givenNameValue);

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.empty());
        given(passwordHasher.hash(new RawPassword(password))).willReturn(hashedPassword);

        service.register(new RegisterCommand(email, password, fullNameValue, givenNameValue));

        then(passwordSecurityService).should().validatePassword(
            new RawPassword(password),
            new Email(email),
            new UserProfileData(fullName, givenName)
        );

        then(registerPersistence).should().save(accountCaptor.capture(), userCaptor.capture());

        var savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getEmail()).isEqualTo(new Email(email));
        assertThat(savedAccount.getPassword()).isEqualTo(hashedPassword);
        assertThat(savedAccount.getRole()).isEqualTo(Role.USER);

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getGivenName()).isEqualTo(givenName);
        assertThat(savedUser.getAccountId()).isEqualTo(savedAccount.getId());
    }

    @Test
    void register_emailAlreadyExists_throwsApplicationException() {

        var email = "bruce.wayne@email.com";
        var password = "SecureP@ss123";
        var existing = Account.reconstitute(
            AccountId.generate(),
            new Email(email),
            new HashedPassword("$2a$12$hashed"),
            Role.USER
        );

        RegisterCommand command = new RegisterCommand(email, password, "Bruce Wayne", "Bruce");

        given(accountRepository.findByEmail(new Email(email))).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.register(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        then(registerPersistence).should(never()).save(any(), any());
    }
}
