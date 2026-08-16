package com.valadir.resilience.support;

import com.valadir.application.port.out.OtpHasher;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes Postgres away in the middle of a request instead of before it.
 *
 * <p>Every flow reads before it writes, so an outage injected beforehand fails on the first lookup and
 * never reaches the transactional write. {@code otpHasher.matches} is the one call in
 * {@code ActivateAccountService} that runs after every read and before
 * {@code accountRepository.activate}, so arming it is what makes the write the failing call — and it is
 * deterministic, unlike a sleep racing the request from another thread.
 */
@TestConfiguration(proxyBeanMethods = false)
public class DatabasePausingOtpHasherConfig {

    // The real bean is ApplicationWiring#otpHasher; by name, because this decorator is @Primary and
    // would otherwise be injected into itself.
    @Bean
    @Primary
    DatabasePausingOtpHasher databasePausingOtpHasher(@Qualifier("otpHasher") OtpHasher delegate) {

        return new DatabasePausingOtpHasher(delegate);
    }

    public static class DatabasePausingOtpHasher implements OtpHasher {

        private final OtpHasher delegate;
        private final AtomicBoolean pauseBeforeNextMatch = new AtomicBoolean(false);

        DatabasePausingOtpHasher(OtpHasher delegate) {

            this.delegate = delegate;
        }

        @Override
        public HashedOtp hash(PlainOtp plainOtp) {

            return delegate.hash(plainOtp);
        }

        // getAndSet: exactly one match pauses, so the retry after the outage runs untouched.
        @Override
        public boolean matches(PlainOtp plainOtp, HashedOtp hashedOtp) {

            if (pauseBeforeNextMatch.getAndSet(false)) {
                ContainerFailure.pause(IsolatedPostgresContainerConfig.container());
            }

            return delegate.matches(plainOtp, hashedOtp);
        }

        @Override
        public void guardTiming() {

            delegate.guardTiming();
        }

        public void pauseDatabaseBeforeNextMatch() {

            pauseBeforeNextMatch.set(true);
        }

        // A test that arms the switch and fails before spending it would otherwise break the next
        // test instead of itself.
        public void reset() {

            pauseBeforeNextMatch.set(false);
        }
    }
}
