package com.valadir.scheduler;

import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PendingActivationAccountPurgeSchedulerTest {

    @Mock
    private PurgeExpiredPendingActivationAccountsUseCase purgeUseCase;

    @InjectMocks
    private PendingActivationAccountPurgeScheduler scheduler;

    @Test
    void purgeExpiredPendingActivationAccounts_delegatesToUseCase() {

        scheduler.purgeExpiredPendingActivationAccounts();

        then(purgeUseCase).should().purge();
    }
}
