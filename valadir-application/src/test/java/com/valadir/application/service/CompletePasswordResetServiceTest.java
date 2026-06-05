package com.valadir.application.service;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.application.port.out.UserRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.PasswordMother;
import com.valadir.test.mother.UserMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CompletePasswordResetServiceTest {

    @Mock
    private PasswordResetVerificationTokenRepository verificationTokenRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private PasswordSecurityService passwordSecurityService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private CompletePasswordResetService service;

    private static final String VERIFICATION_TOKEN = UUID.randomUUID().toString();

    @Test
    void complete_validToken_updatesPasswordAndRevokesTokens() {

        var newPassword = PasswordMother.raw();
        var hashedPassword = PasswordMother.hashed();
        var accountId = AccountId.generate();
        var email = Email.from("bruce.wayne@example.com");
        var account = AccountMother.active().withId(accountId).withEmail(email).build();
        var user = UserMother.builder().withAccountId(accountId).build();
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, newPassword);

        given(verificationTokenRepository.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(userRepository.findByAccountId(accountId)).willReturn(Optional.of(user));
        given(passwordHasher.hash(newPassword)).willReturn(hashedPassword);

        service.complete(command);

        then(passwordSecurityService).should().validatePassword(newPassword, email, user);
        then(accountRepository).should().updatePassword(accountId, hashedPassword);
        then(verificationTokenRepository).should().delete(VERIFICATION_TOKEN);
        then(refreshTokenRepository).should().revokeAllForAccount(accountId);
    }

    @Test
    void complete_tokenNotFound_throwsApplicationException() {

        var newPassword = PasswordMother.raw();
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, newPassword);

        given(verificationTokenRepository.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenRepository).should(never()).delete(any());
        then(refreshTokenRepository).should(never()).revokeAllForAccount(any());
    }

    @Test
    void complete_accountNotFound_throwsApplicationException() {

        var newPassword = PasswordMother.raw();
        var accountId = AccountId.generate();
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, newPassword);

        given(verificationTokenRepository.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenRepository).should(never()).delete(any());
        then(refreshTokenRepository).should(never()).revokeAllForAccount(any());
    }

    @Test
    void complete_userNotFound_throwsApplicationException() {

        var newPassword = PasswordMother.raw();
        var email = Email.from("bruce.wayne@example.com");
        var accountId = AccountId.generate();
        var account = AccountMother.active().withId(accountId).withEmail(email).build();
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, newPassword);

        given(verificationTokenRepository.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(userRepository.findByAccountId(accountId)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenRepository).should(never()).delete(any());
        then(refreshTokenRepository).should(never()).revokeAllForAccount(any());
    }
}
