package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.UserRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.RawPassword;
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NewPasswordValidatorServiceTest {

    private static final Account ACCOUNT = AccountMother.active().build();
    private static final RawPassword NEW_PASSWORD = PasswordMother.raw();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordSecurityService passwordSecurityService;

    @InjectMocks
    private NewPasswordValidatorService validator;

    // The personal data belongs to the owner of the account: checked against anyone else's, it would let
    // the owner's own name through.
    @Test
    void validate_userFound_checksTheNewPasswordAgainstTheOwnersPersonalData() {

        var user = UserMother.builder().withAccountId(ACCOUNT.getId()).build();

        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.of(user));

        validator.validate(ACCOUNT, NEW_PASSWORD);

        then(passwordSecurityService).should().validatePassword(NEW_PASSWORD, ACCOUNT.getEmail(), user);
    }

    @Test
    void validate_userNotFound_throwsApplicationException() {

        given(userRepository.findByAccountId(ACCOUNT.getId())).willReturn(Optional.empty());

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> validator.validate(ACCOUNT, NEW_PASSWORD))
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.DATA_INTEGRITY_ERROR);

        then(passwordSecurityService).shouldHaveNoInteractions();
    }
}
