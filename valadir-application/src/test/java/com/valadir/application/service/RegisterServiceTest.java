package com.valadir.application.service;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.config.VerificationConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
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

import java.time.Duration;
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
    @Mock
    private EmailVerificationPort emailVerificationPort;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private OtpHasher otpHasher;
    @Mock
    private VerificationConfig verificationConfig;
    @InjectMocks
    private RegisterService registerService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<String> otpCaptor;

    @Test
    void register_validData_persistsAccountAndUserAndSendsVerificationCode() {

        var emailValue = "bruce.wayne@emailValue.com";
        var email = new Email(emailValue);
        var rawPasswordValue = "SecureP@ss123";
        var rawPassword = new RawPassword(rawPasswordValue);
        var hashedPassword = new HashedPassword("$2a$12$hashed");
        var fullNameValue = "Bruce Wayne";
        var givenNameValue = "Bruce";
        var fullName = new FullName(fullNameValue);
        var givenName = new GivenName(givenNameValue);
        var hashedOtp = "$argon2id$hashedOtp";
        var tokenTtl = Duration.ofSeconds(900);

        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordHasher.hash(rawPassword)).willReturn(hashedPassword);
        given(otpHasher.hash(any(String.class))).willReturn(hashedOtp);
        given(verificationConfig.tokenTtl()).willReturn(tokenTtl);

        registerService.register(new RegisterCommand(emailValue, rawPasswordValue, fullNameValue, givenNameValue));

        then(passwordSecurityService).should().validatePassword(rawPassword, email, new UserProfileData(fullName, givenName));
        then(registerPersistence).should().save(accountCaptor.capture(), userCaptor.capture());

        var savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getEmail()).isEqualTo(email);
        assertThat(savedAccount.getPassword()).isEqualTo(hashedPassword);
        assertThat(savedAccount.getRole()).isEqualTo(Role.USER);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getGivenName()).isEqualTo(givenName);
        assertThat(savedUser.getAccountId()).isEqualTo(savedAccount.getId());

        then(otpHasher).should().hash(otpCaptor.capture());
        then(otpRepository).should().save(savedAccount.getId(), hashedOtp, tokenTtl);
        then(emailVerificationPort).should().sendVerificationCode(email, otpCaptor.getValue());
    }

    @Test
    void register_emailAlreadyExists_throwsEmailAlreadyExists() {

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
        assertThatThrownBy(() -> registerService.register(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        then(registerPersistence).should(never()).save(any(), any());
        then(otpRepository).should(never()).save(any(), any(), any());
        then(emailVerificationPort).should(never()).sendVerificationCode(any(), any());
    }
}
