package com.valadir.application.service;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenStore;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.UserRepository;
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
import com.valadir.domain.model.UserId;
import com.valadir.domain.model.UserProfileData;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
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
    private PasswordResetVerificationTokenStore verificationTokenStore;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private PasswordSecurityService passwordSecurityService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private CompletePasswordResetService service;

    private static final String VERIFICATION_TOKEN = UUID.randomUUID().toString();
    private static final RawPassword NEW_PASSWORD = new RawPassword("NewP@ssword1");
    private static final HashedPassword HASHED_NEW_PASSWORD = new HashedPassword("$argon2id$newHashed");
    private static final Email EMAIL = new Email("bruce.wayne@example.com");
    private static final FullName FULL_NAME = new FullName("Bruce Wayne");
    private static final GivenName GIVEN_NAME = new GivenName("Batman");

    @Test
    void complete_validToken_updatesPasswordAndRevokesTokens() {

        var accountId = AccountId.generate();
        var account = buildAccount(accountId);
        var user = buildUser(accountId);
        var profileData = new UserProfileData(FULL_NAME, GIVEN_NAME);
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, NEW_PASSWORD.value());

        given(verificationTokenStore.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(userRepository.findByAccountId(accountId)).willReturn(Optional.of(user));
        given(passwordHasher.hash(NEW_PASSWORD)).willReturn(HASHED_NEW_PASSWORD);

        service.complete(command);

        then(passwordSecurityService).should().validatePassword(NEW_PASSWORD, EMAIL, profileData);
        then(accountRepository).should().updatePassword(accountId, HASHED_NEW_PASSWORD);
        then(verificationTokenStore).should().delete(VERIFICATION_TOKEN);
        then(refreshTokenStore).should().revokeAllForAccount(accountId);
    }

    @Test
    void complete_tokenNotFound_throwsApplicationException() {

        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, NEW_PASSWORD.value());

        given(verificationTokenStore.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenStore).should(never()).delete(any());
        then(refreshTokenStore).should(never()).revokeAllForAccount(any());
    }

    @Test
    void complete_accountNotFound_throwsApplicationException() {

        var accountId = AccountId.generate();
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, NEW_PASSWORD.value());

        given(verificationTokenStore.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenStore).should(never()).delete(any());
        then(refreshTokenStore).should(never()).revokeAllForAccount(any());
    }

    @Test
    void complete_userNotFound_throwsApplicationException() {

        var accountId = AccountId.generate();
        var account = buildAccount(accountId);
        var command = new CompletePasswordResetCommand(VERIFICATION_TOKEN, NEW_PASSWORD.value());

        given(verificationTokenStore.resolveAccountId(VERIFICATION_TOKEN)).willReturn(Optional.of(accountId));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(userRepository.findByAccountId(accountId)).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.complete(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_ERROR);

        then(accountRepository).should(never()).updatePassword(any(), any());
        then(verificationTokenStore).should(never()).delete(any());
        then(refreshTokenStore).should(never()).revokeAllForAccount(any());
    }

    private Account buildAccount(AccountId accountId) {

        return Account.reconstitute(accountId, EMAIL, HASHED_NEW_PASSWORD, Role.USER, AccountStatus.ACTIVE);
    }

    private User buildUser(AccountId accountId) {

        return User.reconstitute(UserId.generate(), accountId, FULL_NAME, GIVEN_NAME);
    }
}
