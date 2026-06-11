package com.valadir.web.config;

import com.valadir.web.config.RateLimitProperties.Rule;
import com.valadir.web.config.RateLimitProperties.Strategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPropertiesTest {

    private static final Rule RULE = new Rule(
        "/api/auth/login",
        Strategy.IP,
        10,
        Duration.ofSeconds(60)
    );

    @Test
    void constructor_validRules_preservesEnabledFlagAndRules() {

        var properties = new RateLimitProperties(true, List.of(RULE));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.rules()).containsExactly(RULE);
    }

    @Test
    void constructor_nullRules_defaultsToEmptyList() {

        var properties = new RateLimitProperties(false, null);

        assertThat(properties.rules()).isEmpty();
    }

    @Test
    void constructor_mutableSourceList_storesDefensiveCopy() {

        var mutableRules = new ArrayList<>(List.of(RULE));
        var properties = new RateLimitProperties(true, mutableRules);

        mutableRules.clear();

        assertThat(properties.rules()).containsExactly(RULE);
    }
}
