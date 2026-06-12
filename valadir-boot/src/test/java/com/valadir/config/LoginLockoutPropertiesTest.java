package com.valadir.config;

import com.valadir.config.LoginLockoutProperties.ThresholdProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LoginLockoutPropertiesTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final ThresholdProperties THRESHOLD = new ThresholdProperties(3, Duration.ofMinutes(5));

    @Test
    void constructor_nullThresholds_throwsIllegalArgument() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutProperties(WINDOW, null));
    }

    @Test
    void constructor_emptyThresholds_throwsIllegalArgument() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutProperties(WINDOW, List.of()));
    }

    @Test
    void constructor_mutableSourceList_storesDefensiveCopy() {

        var mutableThresholds = new ArrayList<>(List.of(THRESHOLD));
        var properties = new LoginLockoutProperties(WINDOW, mutableThresholds);

        mutableThresholds.clear();

        assertThat(properties.thresholds()).containsExactly(THRESHOLD);
    }
}
