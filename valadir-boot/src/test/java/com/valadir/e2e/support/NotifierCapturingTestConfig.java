package com.valadir.e2e.support;

import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replaces the driven notifier ports with capturing doubles, so a test can read the OTP delivered
 * to an email and drive the flows without SMTP. The real JavaMail adapters keep their own unit
 * tests, so nothing is left uncovered.
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

    /**
     * What each double delivers to, and the switch that fails the next send: an SMTP outage is
     * driven at the port, never by breaking a real mail server. It throws the real adapter's
     * {@link InfrastructureException}, so the flow under test cannot tell the two apart.
     */
    private static final class Mailbox<T> {

        private final Map<String, T> deliveredByEmail = new ConcurrentHashMap<>();
        private final AtomicBoolean nextSendFails = new AtomicBoolean(false);

        // getAndSet: exactly one send fails, so a test asserting the retry need not disarm anything.
        void deliver(Email email, T delivered) {

            if (nextSendFails.getAndSet(false)) {
                throw new InfrastructureException("Mail server unavailable");
            }

            deliveredByEmail.put(email.value(), delivered);
        }

        Optional<T> lastFor(String email) {

            return Optional.ofNullable(deliveredByEmail.get(email));
        }

        boolean isEmpty() {

            return deliveredByEmail.isEmpty();
        }

        void failNextSend() {

            nextSendFails.set(true);
        }

        // Clears the switch too: a test that arms it and fails before spending it would otherwise
        // break the next test instead of itself.
        void clear() {

            deliveredByEmail.clear();
            nextSendFails.set(false);
        }
    }

    public static class CapturingAccountActivationNotifier implements AccountActivationNotifier {

        private final Mailbox<PlainOtp> mailbox = new Mailbox<>();

        @Override
        public void sendActivationCode(Email email, PlainOtp plainOtp) {

            mailbox.deliver(email, plainOtp);
        }

        public Optional<PlainOtp> lastOtpFor(String email) {

            return mailbox.lastFor(email);
        }

        public void failNextSend() {

            mailbox.failNextSend();
        }

        public void reset() {

            mailbox.clear();
        }
    }

    public static class CapturingPasswordResetNotifier implements PasswordResetNotifier {

        private final Mailbox<PlainOtp> mailbox = new Mailbox<>();

        @Override
        public void sendResetCode(Email email, PlainOtp plainOtp) {

            mailbox.deliver(email, plainOtp);
        }

        public Optional<PlainOtp> lastOtpFor(String email) {

            return mailbox.lastFor(email);
        }

        public void failNextSend() {

            mailbox.failNextSend();
        }

        public void reset() {

            mailbox.clear();
        }
    }

    /**
     * Synchronous: {@code @Async} sits on the adapter this bean replaces, not on the port. So a
     * failure armed with {@link #failNextSend()} reaches the use case, where the login guards it.
     */
    public static class CapturingAccountLockedNotifier implements AccountLockedNotifier {

        private final Mailbox<Duration> mailbox = new Mailbox<>();

        @Override
        public void notifyAccountLocked(Email email, Duration lockoutDuration) {

            mailbox.deliver(email, lockoutDuration);
        }

        public Optional<Duration> lastLockoutFor(String email) {

            return mailbox.lastFor(email);
        }

        public boolean capturedNothing() {

            return mailbox.isEmpty();
        }

        public void failNextSend() {

            mailbox.failNextSend();
        }

        public void reset() {

            mailbox.clear();
        }
    }
}
