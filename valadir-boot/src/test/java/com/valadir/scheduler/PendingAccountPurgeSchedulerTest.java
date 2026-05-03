package com.valadir.scheduler;

import com.valadir.application.port.in.PurgeExpiredPendingAccountsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PendingAccountPurgeSchedulerTest {

    @Mock
    private PurgeExpiredPendingAccountsUseCase purgeUseCase;

    @InjectMocks
    private PendingAccountPurgeScheduler scheduler;

    @Test
    void purgeExpiredPendingAccounts_delegatesToUseCase() {

        scheduler.purgeExpiredPendingAccounts();

        then(purgeUseCase).should().purge();
    }
}
