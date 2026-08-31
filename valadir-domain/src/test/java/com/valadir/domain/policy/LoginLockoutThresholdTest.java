package com.valadir.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LoginLockoutThresholdTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void constructor_nonPositiveMinFailures_throwsIllegalArgumentException(int minFailures) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutThreshold(minFailures, Duration.ofSeconds(30)));
    }

    @ParameterizedTest
    @MethodSource("nonPositiveLockouts")
    void constructor_nonPositiveLockout_throwsIllegalArgumentException(Duration lockout) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutThreshold(3, lockout));
    }

    @Test
    void constructor_nullLockout_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutThreshold(3, null));
    }

    @Test
    void constructor_validValues_createsInstance() {

        var threshold = new LoginLockoutThreshold(3, Duration.ofSeconds(30));

        assertThat(threshold.minFailures()).isEqualTo(3);
        assertThat(threshold.lockout()).isEqualTo(Duration.ofSeconds(30));
    }

    static Stream<Duration> nonPositiveLockouts() {

        return Stream.of(Duration.ZERO, Duration.ofSeconds(-1));
    }
}
