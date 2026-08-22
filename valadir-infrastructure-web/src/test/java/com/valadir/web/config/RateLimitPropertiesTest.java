package com.valadir.web.config;

import com.valadir.web.config.RateLimitProperties.Rule;
import com.valadir.web.config.RateLimitProperties.Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RateLimitPropertiesTest {

    private static final Rule RULE = new Rule(
        "/api/auth/login",
        Strategy.IP,
        10,
        Duration.ofSeconds(60)
    );

    @Test
    void constructor_nullRulesWhileDisabled_defaultsToEmptyList() {

        var properties = new RateLimitProperties(false, null);

        assertThat(properties.rules()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_enabledWithoutRules_throws(List<Rule> rules) {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new RateLimitProperties(true, rules));
    }

    @Test
    void constructor_mutableSourceList_storesDefensiveCopy() {

        var mutableRules = new ArrayList<>(List.of(RULE));
        var properties = new RateLimitProperties(true, mutableRules);

        mutableRules.clear();

        assertThat(properties.rules()).containsExactly(RULE);
    }
}
