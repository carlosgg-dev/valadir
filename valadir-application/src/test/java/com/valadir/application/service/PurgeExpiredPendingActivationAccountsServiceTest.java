package com.valadir.application.service;

import com.valadir.application.config.PendingActivationAccountPurgeConfig;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
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
class PurgeExpiredPendingActivationAccountsServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-02T12:00:00Z");
    private static final Duration GRACE_PERIOD = Duration.ofHours(72);
    private static final Instant EXPECTED_CUTOFF = FIXED_NOW.minus(GRACE_PERIOD);

    @Mock
    private ExpiredPendingActivationAccountCleaner cleaner;

    private PurgeExpiredPendingActivationAccountsService service;

    @BeforeEach
    void setUp() {

        var config = new PendingActivationAccountPurgeConfig(GRACE_PERIOD);
        var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new PurgeExpiredPendingActivationAccountsService(cleaner, config, clock);
    }

    @Test
    void purge_callsCleanerWithCutoffDerivedFromGracePeriod() {

        given(cleaner.delete(EXPECTED_CUTOFF)).willReturn(3);

        service.purge();

        then(cleaner).should().delete(EXPECTED_CUTOFF);
    }
}
