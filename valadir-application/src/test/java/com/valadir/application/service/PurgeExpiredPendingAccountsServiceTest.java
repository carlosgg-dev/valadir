package com.valadir.application.service;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PurgeExpiredPendingAccountsServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-02T12:00:00Z");
    private static final Duration GRACE_PERIOD = Duration.ofHours(72);
    private static final Instant EXPECTED_CUTOFF = FIXED_NOW.minus(GRACE_PERIOD);

    @Mock
    private ExpiredPendingAccountCleaner cleaner;

    private PurgeExpiredPendingAccountsService service;

    @BeforeEach
    void setUp() {

        var config = new VerificationConfig(Duration.ofMinutes(15), GRACE_PERIOD);
        var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new PurgeExpiredPendingAccountsService(cleaner, config, clock);
    }

    @Test
    void purge_callsCleanerWithCutoffDerivedFromGracePeriod() {

        given(cleaner.delete(EXPECTED_CUTOFF)).willReturn(3);

        service.purge();

        then(cleaner).should().delete(EXPECTED_CUTOFF);
    }
}
