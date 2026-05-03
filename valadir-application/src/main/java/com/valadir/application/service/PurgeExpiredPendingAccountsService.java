package com.valadir.application.service;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.in.PurgeExpiredPendingAccountsUseCase;
import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class PurgeExpiredPendingAccountsService implements PurgeExpiredPendingAccountsUseCase {

    private static final Logger log = LoggerFactory.getLogger(PurgeExpiredPendingAccountsService.class);

    private final ExpiredPendingAccountCleaner cleaner;
    private final VerificationConfig verificationConfig;
    private final Clock clock;

    public PurgeExpiredPendingAccountsService(ExpiredPendingAccountCleaner cleaner, VerificationConfig verificationConfig, Clock clock) {

        this.cleaner = cleaner;
        this.verificationConfig = verificationConfig;
        this.clock = clock;
    }

    @Override
    public void purge() {

        Instant cutoff = Instant.now(clock).minus(verificationConfig.accountGracePeriod());
        int deleted = cleaner.delete(cutoff);
        log.info("Purged {} expired PENDING_VERIFICATION account(s) older than {}", deleted, cutoff);
    }
}
