package com.valadir.config;

import com.valadir.config.LoginLockoutProperties.ThresholdProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginLockoutPropertiesTest {

    @Test
    void constructor_mutableSourceList_storesDefensiveCopy() {

        var window = Duration.ofMinutes(15);
        var challengeThreshold = 2;
        var threshold = new ThresholdProperties(3, Duration.ofMinutes(5));
        var mutableThresholds = new ArrayList<>(List.of(threshold));
        var properties = new LoginLockoutProperties(window, challengeThreshold, mutableThresholds);

        mutableThresholds.clear();

        assertThat(properties.thresholds()).containsExactly(threshold);
    }
}
