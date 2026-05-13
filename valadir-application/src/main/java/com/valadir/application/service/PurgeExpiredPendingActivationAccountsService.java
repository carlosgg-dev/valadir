package com.valadir.application.service;

import com.valadir.application.config.PendingActivationAccountPurgeConfig;
import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class PurgeExpiredPendingActivationAccountsService implements PurgeExpiredPendingActivationAccountsUseCase {

    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredPendingActivationAccountsService.class);

    private final ExpiredPendingActivationAccountCleaner cleaner;
    private final PendingActivationAccountPurgeConfig purgeConfig;
    private final Clock clock;

    public PurgeExpiredPendingActivationAccountsService(ExpiredPendingActivationAccountCleaner cleaner, PendingActivationAccountPurgeConfig purgeConfig, Clock clock) {

        this.cleaner = cleaner;
        this.purgeConfig = purgeConfig;
        this.clock = clock;
    }

    @Override
    public void purge() {

        Instant cutoff = Instant.now(clock).minus(purgeConfig.accountGracePeriod());
        int deleted = cleaner.delete(cutoff);
        log.info("Purged {} expired account(s) pending activation older than {}", deleted, cutoff);
    }
}
