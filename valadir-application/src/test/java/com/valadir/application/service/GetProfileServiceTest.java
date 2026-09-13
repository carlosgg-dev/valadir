package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.UserRepository;
import com.valadir.application.result.ProfileResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.User;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetProfileServiceTest {

    // Deliberately not the fallback language: a profile answering EN regardless would still pass.
    private static final Account ACCOUNT = AccountMother.active()
        .withLanguage(Language.ES)
        .build();

    private static final User USER = UserMother.builder()
        .withAccountId(ACCOUNT.getId())
        .build();

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetProfileService service;

    @Test
    void getProfile_existingAccount_returnsItsProfile() {

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.of(USER));

        var result = service.getProfile(ACCOUNT.getId().value().toString());

        assertThat(result).isEqualTo(new ProfileResult(
            ACCOUNT.getEmail().value(),
            USER.getFullName().value(),
            USER.getGivenName().value(),
            Language.ES.tag()
        ));
    }

    @Test
    void getProfile_accountNotFound_throwsDataIntegrityError() {

        var accountId = ACCOUNT.getId().value().toString();

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.getProfile(accountId))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);
    }

    @Test
    void getProfile_userNotFound_throwsDataIntegrityError() {

        var accountId = ACCOUNT.getId().value().toString();

        given(accountRepository.findById(ACCOUNT.getId())).willReturn(Optional.of(ACCOUNT));
        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.getProfile(accountId))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);
    }
}
