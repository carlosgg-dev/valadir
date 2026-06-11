package com.valadir.scheduler;

import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.support.CronExpression;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    // The test profile disables the schedule with "-", so no other test parses
    // the production cron. An invalid value would only surface at boot in production.
    @Test
    void purgeCron_productionConfiguration_isParseableCronExpression() {

        var properties = new YamlPropertiesFactoryBean();
        properties.setResources(new ClassPathResource("application.yml"));

        var cron = requireNonNull(properties.getObject()).getProperty("scheduler.pending-activation-account.purge-cron");

        assertThat(cron).isNotBlank();
        assertThatCode(() -> CronExpression.parse(cron)).doesNotThrowAnyException();
    }
}
