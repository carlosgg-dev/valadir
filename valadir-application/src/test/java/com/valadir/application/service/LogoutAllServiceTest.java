package com.valadir.application.service;

import com.valadir.application.command.LogoutAllCommand;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class LogoutAllServiceTest {

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Redis error");

    @Mock
    private AccountTokensInvalidator accountTokensInvalidator;

    @InjectMocks
    private LogoutAllService service;

    @Test
    void logoutAll_success_invalidatesEverySessionOfTheAccount() {

        var accountId = AccountId.generate();

        service.logoutAll(new LogoutAllCommand(accountId.value().toString()));

        then(accountTokensInvalidator).should().invalidateAll(accountId);
    }

    // The password reset swallows this same failure because there the revocation is cleanup after
    // the password has already changed. Here it is the whole operation: a silent 204 would tell the
    // user their devices are signed out while every session stays alive.
    @Test
    void logoutAll_invalidationFails_propagatesInfrastructureException() {

        var accountId = AccountId.generate();

        var command = new LogoutAllCommand(accountId.value().toString());

        willThrow(INFRA_ERROR)
            .given(accountTokensInvalidator).invalidateAll(accountId);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> service.logoutAll(command))
            .isSameAs(INFRA_ERROR);
    }
}
