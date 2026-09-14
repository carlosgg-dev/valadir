package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.test.mother.AccountMother;
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
class EmailHolderResolverServiceTest {

    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private EmailHolderResolverService emailHolderResolver;

    @Test
    void replaceableHolderFor_noAccountHoldsTheEmail_returnsEmpty() {

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThat(emailHolderResolver.replaceableHolderFor(EMAIL)).isEmpty();
    }

    @Test
    void replaceableHolderFor_pendingAccountHoldsTheEmail_returnsItsId() {

        var pendingAccountId = AccountId.generate();
        var pendingHolder = AccountMother.pendingActivation()
            .withId(pendingAccountId)
            .withEmail(EMAIL)
            .build();

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.of(pendingHolder));

        assertThat(emailHolderResolver.replaceableHolderFor(EMAIL)).hasValue(pendingAccountId);
    }

    @Test
    void replaceableHolderFor_activeAccountHoldsTheEmail_throwsEmailAlreadyExists() {

        var activeHolder = AccountMother.active().withEmail(EMAIL).build();

        given(accountRepository.findByEmail(EMAIL)).willReturn(Optional.of(activeHolder));

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> emailHolderResolver.replaceableHolderFor(EMAIL))
            .extracting("errorCode")
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
