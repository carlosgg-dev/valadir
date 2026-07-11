package com.valadir.e2e.support;

import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the driven notifier ports with capturing doubles so E2E tests can retrieve the
 * OTP delivered to a given email and complete the activation/reset flows without SMTP.
 * The real JavaMail adapters keep their own unit tests, so no coverage gap is introduced.
 */
@TestConfiguration(proxyBeanMethods = false)
public class NotifierCapturingTestConfig {

    @Bean
    @Primary
    CapturingAccountActivationNotifier capturingAccountActivationNotifier() {

        return new CapturingAccountActivationNotifier();
    }

    @Bean
    @Primary
    CapturingPasswordResetNotifier capturingPasswordResetNotifier() {

        return new CapturingPasswordResetNotifier();
    }

    @Bean
    @Primary
    CapturingAccountLockedNotifier capturingAccountLockedNotifier() {

        return new CapturingAccountLockedNotifier();
    }

    public static class CapturingAccountActivationNotifier implements AccountActivationNotifier {

        private final Map<String, PlainOtp> otpByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendActivationCode(Email email, PlainOtp plainOtp) {

            otpByEmail.put(email.value(), plainOtp);
        }

        public Optional<PlainOtp> lastOtpFor(String email) {

            return Optional.ofNullable(otpByEmail.get(email));
        }

        public void reset() {

            otpByEmail.clear();
        }
    }

    public static class CapturingPasswordResetNotifier implements PasswordResetNotifier {

        private final Map<String, PlainOtp> otpByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendResetCode(Email email, PlainOtp plainOtp) {

            otpByEmail.put(email.value(), plainOtp);
        }

        public Optional<PlainOtp> lastOtpFor(String email) {

            return Optional.ofNullable(otpByEmail.get(email));
        }

        public void reset() {

            otpByEmail.clear();
        }
    }

    /**
     * The real adapter is {@code @Async}: captures happen on a {@code notif-async-*} thread,
     * so tests must await them with Awaitility instead of asserting immediately.
     */
    public static class CapturingAccountLockedNotifier implements AccountLockedNotifier {

        private final Map<String, Duration> lockoutByEmail = new ConcurrentHashMap<>();

        @Override
        public void notifyAccountLocked(Email email, Duration lockoutDuration) {

            lockoutByEmail.put(email.value(), lockoutDuration);
        }

        public Optional<Duration> lastLockoutFor(String email) {

            return Optional.ofNullable(lockoutByEmail.get(email));
        }

        public boolean capturedNothing() {

            return lockoutByEmail.isEmpty();
        }

        public void reset() {

            lockoutByEmail.clear();
        }
    }
}
