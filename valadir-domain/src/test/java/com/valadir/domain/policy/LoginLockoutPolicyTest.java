package com.valadir.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LoginLockoutPolicyTest {

    private static final List<LoginLockoutThreshold> VALID_THRESHOLDS = List.of(
        new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
        new LoginLockoutThreshold(5, Duration.ofSeconds(120)),
        new LoginLockoutThreshold(7, Duration.ofSeconds(600))
    );

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(Duration.ofHours(1), VALID_THRESHOLDS);

    @Test
    void constructor_storesWindowAndThresholds() {

        var window = Duration.ofMinutes(30);
        var policy = new LoginLockoutPolicy(window, VALID_THRESHOLDS);

        assertThat(policy.attemptsWindow()).isEqualTo(window);
        assertThat(policy.thresholds()).containsExactlyElementsOf(VALID_THRESHOLDS);
    }

    @Test
    void constructor_thresholdsAreImmutable() {

        var thresholds = POLICY.thresholds();
        var extra = new LoginLockoutThreshold(10, Duration.ofSeconds(1000));

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> thresholds.add(extra));
    }

    @ParameterizedTest
    @MethodSource("nonPositiveAttemptsWindows")
    void constructor_nonPositiveAttemptsWindow_throwsIllegalArgumentException(Duration window) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(window, VALID_THRESHOLDS));
    }

    @Test
    void constructor_duplicateMinFailures_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(
                Duration.ofHours(1),
                List.of(
                    new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
                    new LoginLockoutThreshold(3, Duration.ofSeconds(60))
                )
            ));
    }

    @Test
    void constructor_descendingLockouts_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(
                Duration.ofHours(1),
                List.of(
                    new LoginLockoutThreshold(3, Duration.ofSeconds(600)),
                    new LoginLockoutThreshold(5, Duration.ofSeconds(30))
                )
            ));
    }

    @Test
    void constructor_equalLockouts_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(
                Duration.ofHours(1),
                List.of(
                    new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
                    new LoginLockoutThreshold(5, Duration.ofSeconds(30))
                )
            ));
    }

    @Test
    void lockoutFor_below3Failures_returnsZero() {

        assertThat(POLICY.lockoutFor(0)).isEqualTo(Duration.ZERO);
        assertThat(POLICY.lockoutFor(1)).isEqualTo(Duration.ZERO);
        assertThat(POLICY.lockoutFor(2)).isEqualTo(Duration.ZERO);
    }

    @Test
    void lockoutFor_at3Failures_returns30Seconds() {

        assertThat(POLICY.lockoutFor(3)).isEqualTo(Duration.ofSeconds(30));
        assertThat(POLICY.lockoutFor(4)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void lockoutFor_at5Failures_returns2Minutes() {

        assertThat(POLICY.lockoutFor(5)).isEqualTo(Duration.ofSeconds(120));
        assertThat(POLICY.lockoutFor(6)).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void lockoutFor_at7OrMoreFailures_returns10MinutesCeiling() {

        assertThat(POLICY.lockoutFor(7)).isEqualTo(Duration.ofSeconds(600));
        assertThat(POLICY.lockoutFor(10)).isEqualTo(Duration.ofSeconds(600));
        assertThat(POLICY.lockoutFor(100)).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void lockoutFor_unorderedThresholds_returnsCorrectLockout() {

        var policy = new LoginLockoutPolicy(
            Duration.ofHours(1),
            List.of(
                new LoginLockoutThreshold(7, Duration.ofSeconds(600)),
                new LoginLockoutThreshold(3, Duration.ofSeconds(30))
            )
        );

        assertThat(policy.lockoutFor(3)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.lockoutFor(7)).isEqualTo(Duration.ofSeconds(600));
    }

    static Stream<Duration> nonPositiveAttemptsWindows() {

        return Stream.of(Duration.ZERO, Duration.ofSeconds(-1));
    }
}
