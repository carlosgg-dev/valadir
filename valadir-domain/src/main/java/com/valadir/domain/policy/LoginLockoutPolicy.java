package com.valadir.domain.policy;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record LoginLockoutPolicy(
    Duration attemptsWindow,
    int challengeThreshold,
    List<LoginLockoutThreshold> thresholds) {

    public LoginLockoutPolicy {

        validatePositiveAttemptsWindow(attemptsWindow);
        validateThresholdsPresent(thresholds);
        validateNoDuplicateMinFailures(thresholds);
        validateAscendingLockouts(thresholds);
        validateChallengeThresholdBelowFirstTier(challengeThreshold, thresholds);

        thresholds = List.copyOf(thresholds);
    }

    public Duration lockoutFor(long failureCount) {

        return thresholds.stream()
            .filter(threshold -> failureCount >= threshold.minFailures())
            .map(LoginLockoutThreshold::lockout)
            .max(Comparator.naturalOrder())
            .orElse(Duration.ZERO);
    }

    public LoginAttemptDecision decideByCount(long failureCount) {

        return failureCount >= challengeThreshold
            ? new LoginAttemptDecision.ChallengeRequired()
            : new LoginAttemptDecision.Allowed();
    }

    private static void validatePositiveAttemptsWindow(Duration attemptsWindow) {

        if (!attemptsWindow.isPositive()) {
            throw new IllegalArgumentException("Login attempts window must be a positive duration");
        }
    }

    private static void validateThresholdsPresent(List<LoginLockoutThreshold> thresholds) {

        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("Lockout policy requires at least one threshold");
        }
    }

    private static void validateNoDuplicateMinFailures(List<LoginLockoutThreshold> thresholds) {

        Set<Integer> seen = thresholds.stream()
            .map(LoginLockoutThreshold::minFailures)
            .collect(Collectors.toSet());

        if (seen.size() != thresholds.size()) {
            throw new IllegalArgumentException("Lockout thresholds must not have duplicate minFailures values");
        }
    }

    private static void validateAscendingLockouts(List<LoginLockoutThreshold> thresholds) {

        List<LoginLockoutThreshold> sorted = thresholds.stream()
            .sorted(Comparator.comparingInt(LoginLockoutThreshold::minFailures))
            .toList();

        boolean notStrictlyAscending = IntStream.range(1, sorted.size())
            .anyMatch(i -> sorted.get(i).lockout().compareTo(sorted.get(i - 1).lockout()) <= 0);

        if (notStrictlyAscending) {
            throw new IllegalArgumentException("Lockout durations must be strictly ascending when ordered by minFailures");
        }
    }

    private static void validateChallengeThresholdBelowFirstTier(int challengeThreshold, List<LoginLockoutThreshold> thresholds) {

        if (challengeThreshold <= 0) {
            throw new IllegalArgumentException("Challenge threshold must be a positive number of failures");
        }

        // The CAPTCHA step-up must trigger before any hard lockout, otherwise it is unreachable.
        boolean challengeReachesAnyTier = thresholds.stream()
            .anyMatch(threshold -> challengeThreshold >= threshold.minFailures());

        if (challengeReachesAnyTier) {
            throw new IllegalArgumentException("Challenge threshold must be below the first lockout tier so the step-up precedes any hard lockout");
        }
    }
}
