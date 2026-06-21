package com.valadir.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    private static final int CHALLENGE_THRESHOLD = 2;

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(Duration.ofHours(1), CHALLENGE_THRESHOLD, VALID_THRESHOLDS);

    @ParameterizedTest
    @MethodSource("nonPositiveAttemptsWindows")
    void constructor_nonPositiveAttemptsWindow_throwsIllegalArgumentException(Duration window) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(window, CHALLENGE_THRESHOLD, VALID_THRESHOLDS));
    }

    @Test
    void constructor_nullThresholds_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(Duration.ofHours(1), CHALLENGE_THRESHOLD, null));
    }

    @Test
    void constructor_emptyThresholds_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(Duration.ofHours(1), CHALLENGE_THRESHOLD, List.of()));
    }

    @Test
    void constructor_duplicateMinFailures_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(
                Duration.ofHours(1),
                CHALLENGE_THRESHOLD,
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
                CHALLENGE_THRESHOLD,
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
                CHALLENGE_THRESHOLD,
                List.of(
                    new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
                    new LoginLockoutThreshold(5, Duration.ofSeconds(30))
                )
            ));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void constructor_nonPositiveChallengeThreshold_throwsIllegalArgumentException(int challengeThreshold) {

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(Duration.ofHours(1), challengeThreshold, VALID_THRESHOLDS));
    }

    @Test
    void constructor_challengeThresholdNotBelowFirstTier_throwsIllegalArgumentException() {

        int firstTierMinFailures = VALID_THRESHOLDS.getFirst().minFailures();

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new LoginLockoutPolicy(Duration.ofHours(1), firstTierMinFailures, VALID_THRESHOLDS));
    }

    @Test
    void constructor_thresholdsAreImmutable() {

        var thresholds = POLICY.thresholds();
        var extra = new LoginLockoutThreshold(10, Duration.ofSeconds(1000));

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> thresholds.add(extra));
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
            CHALLENGE_THRESHOLD,
            List.of(
                new LoginLockoutThreshold(7, Duration.ofSeconds(600)),
                new LoginLockoutThreshold(3, Duration.ofSeconds(30))
            )
        );

        assertThat(policy.lockoutFor(3)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.lockoutFor(7)).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void decideByCount_belowChallengeThreshold_returnsAllowed() {

        assertThat(POLICY.decideByCount(CHALLENGE_THRESHOLD - 1))
            .isInstanceOf(LoginAttemptDecision.Allowed.class);
    }

    @Test
    void decideByCount_atChallengeThreshold_returnsChallengeRequired() {

        assertThat(POLICY.decideByCount(CHALLENGE_THRESHOLD))
            .isInstanceOf(LoginAttemptDecision.ChallengeRequired.class);
    }

    @Test
    void decideByCount_aboveChallengeThreshold_returnsChallengeRequired() {

        assertThat(POLICY.decideByCount(CHALLENGE_THRESHOLD + 1))
            .isInstanceOf(LoginAttemptDecision.ChallengeRequired.class);
    }

    static Stream<Duration> nonPositiveAttemptsWindows() {

        return Stream.of(Duration.ZERO, Duration.ofSeconds(-1));
    }
}
