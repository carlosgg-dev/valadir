package com.valadir.scheduler;

import com.valadir.application.port.in.PurgeExpiredPendingAccountsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingAccountPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingAccountPurgeScheduler.class);

    private final PurgeExpiredPendingAccountsUseCase purgeUseCase;

    public PendingAccountPurgeScheduler(PurgeExpiredPendingAccountsUseCase purgeUseCase) {

        this.purgeUseCase = purgeUseCase;
    }

    @Scheduled(cron = "${scheduler.pending-account.purge-cron}")
    public void purgeExpiredPendingAccounts() {

        log.info("Starting scheduled purge of expired PENDING_VERIFICATION accounts");
        purgeUseCase.purge();
    }
}
