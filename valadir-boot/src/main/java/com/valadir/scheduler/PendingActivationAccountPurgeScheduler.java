package com.valadir.scheduler;

import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingActivationAccountPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingActivationAccountPurgeScheduler.class);

    private final PurgeExpiredPendingActivationAccountsUseCase purgeUseCase;

    public PendingActivationAccountPurgeScheduler(PurgeExpiredPendingActivationAccountsUseCase purgeUseCase) {

        this.purgeUseCase = purgeUseCase;
    }

    @Scheduled(cron = "${scheduler.pending-activation-account.purge-cron}")
    public void purgeExpiredPendingActivationAccounts() {

        log.info("Starting scheduled purge of expired accounts pending activation");
        purgeUseCase.purge();
    }
}
