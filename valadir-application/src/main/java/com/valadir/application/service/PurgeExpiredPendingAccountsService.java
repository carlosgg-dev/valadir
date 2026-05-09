package com.valadir.application.service;

import com.valadir.application.config.PendingAccountPurgeConfig;
import com.valadir.application.port.in.PurgeExpiredPendingAccountsUseCase;
import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class PurgeExpiredPendingAccountsService implements PurgeExpiredPendingAccountsUseCase {

    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredPendingAccountsService.class);

    private final ExpiredPendingAccountCleaner cleaner;
    private final PendingAccountPurgeConfig purgeConfig;
    private final Clock clock;

    public PurgeExpiredPendingAccountsService(ExpiredPendingAccountCleaner cleaner, PendingAccountPurgeConfig purgeConfig, Clock clock) {

        this.cleaner = cleaner;
        this.purgeConfig = purgeConfig;
        this.clock = clock;
    }

    @Override
    public void purge() {

        Instant cutoff = Instant.now(clock).minus(purgeConfig.accountGracePeriod());
        int deleted = cleaner.delete(cutoff);
        log.info("Purged {} expired PENDING_VERIFICATION account(s) older than {}", deleted, cutoff);
    }
}
