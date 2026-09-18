package com.valadir.config;

import com.valadir.config.LoginLockoutProperties.ThresholdProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LoginLockoutPropertiesTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CHALLENGE_THRESHOLD = 2;
    private static final ThresholdProperties THRESHOLD = new ThresholdProperties(3, Duration.ofMinutes(5));

    @Test
    void constructor_mutableSourceList_storesDefensiveCopy() {

        var mutableThresholds = new ArrayList<>(List.of(THRESHOLD));
        var properties = new LoginLockoutProperties(WINDOW, CHALLENGE_THRESHOLD, mutableThresholds);

        mutableThresholds.clear();

        assertThat(properties.thresholds()).containsExactly(THRESHOLD);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_withoutThresholds_throwsNamingTheKey(List<ThresholdProperties> thresholds) {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new LoginLockoutProperties(WINDOW, CHALLENGE_THRESHOLD, thresholds))
            .withMessageContaining("auth.lockout.thresholds");
    }
}
