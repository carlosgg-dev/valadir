package com.valadir.application.service;

import com.valadir.application.command.UpdateProfileCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.UpdateProfilePersistence;
import com.valadir.application.port.out.UserRepository;
import com.valadir.application.result.ProfileResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.User;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UpdateProfileServiceTest {

    private static final Account ACCOUNT = AccountMother.active().build();

    private static final User USER = UserMother.builder()
        .withAccountId(ACCOUNT.getId())
        .build();

    private static final String ACCOUNT_ID = ACCOUNT.getId().value().toString();
    private static final String NEW_FULL_NAME = "Bruce Thomas Wayne";
    private static final String NEW_GIVEN_NAME = "Matches Malone";
    // Not the account's current language: a change that never reached the account would still pass.
    private static final String NEW_LANGUAGE = "es";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UpdateProfilePersistence updateProfilePersistence;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    private UpdateProfileService service;

    @Test
    void update_validProfile_persistsTheChangesAndReturnsTheUpdatedProfile() {

        var command = new UpdateProfileCommand(ACCOUNT_ID, NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.of(USER));

        var result = service.update(command);

        then(updateProfilePersistence).should().update(accountCaptor.capture(), userCaptor.capture());

        var persistedAccount = accountCaptor.getValue();
        assertThat(persistedAccount.getId()).isEqualTo(ACCOUNT.getId());
        assertThat(persistedAccount.getLanguage()).isEqualTo(Language.ES);

        var persistedUser = userCaptor.getValue();
        assertThat(persistedUser.getId()).isEqualTo(USER.getId());
        assertThat(persistedUser.getFullName()).isEqualTo(FullName.from(NEW_FULL_NAME));
        assertThat(persistedUser.getGivenName()).isEqualTo(GivenName.from(NEW_GIVEN_NAME));

        assertThat(result).isEqualTo(new ProfileResult(ACCOUNT.getEmail().value(), NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE));
    }

    // Rejected before anything is read: a request that cannot be stored never reaches Postgres.
    @Test
    void update_unsupportedLanguage_throwsInvalidFieldWithoutTouchingTheProfile() {

        var command = new UpdateProfileCommand(ACCOUNT_ID, NEW_FULL_NAME, NEW_GIVEN_NAME, "fr");

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.update(command))
            .withCauseInstanceOf(DomainException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.INVALID_FIELD);

        then(accountRepository).shouldHaveNoInteractions();
        then(updateProfilePersistence).shouldHaveNoInteractions();
    }

    @Test
    void update_accountNotFound_throwsDataIntegrityErrorWithoutPersisting() {

        var command = new UpdateProfileCommand(ACCOUNT_ID, NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.update(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(updateProfilePersistence).shouldHaveNoInteractions();
    }

    @Test
    void update_userNotFound_throwsDataIntegrityErrorWithoutPersisting() {

        var command = new UpdateProfileCommand(ACCOUNT_ID, NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE);

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.update(command))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(updateProfilePersistence).shouldHaveNoInteractions();
    }
}
