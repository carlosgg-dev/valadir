package com.valadir.resilience.support;

import com.valadir.application.port.out.PasswordHasher;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes Redis away in the middle of a request instead of before it.
 *
 * <p>{@code hash} is the last call before the session revocation in {@code CompletePasswordResetService}:
 * pausing there fails the revocation, pausing earlier fails the token lookup.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisPausingPasswordHasherConfig {

    // The real bean is SecurityWiring#passwordHasher; by name, because this decorator is @Primary and
    // would otherwise be injected into itself.
    @Bean
    @Primary
    RedisPausingPasswordHasher redisPausingPasswordHasher(@Qualifier("passwordHasher") PasswordHasher delegate) {

        return new RedisPausingPasswordHasher(delegate);
    }

    public static class RedisPausingPasswordHasher implements PasswordHasher {

        private final PasswordHasher delegate;
        private final AtomicBoolean pauseBeforeNextHash = new AtomicBoolean(false);

        RedisPausingPasswordHasher(PasswordHasher delegate) {

            this.delegate = delegate;
        }

        // getAndSet: exactly one hash pauses, so the retry after the outage runs untouched.
        @Override
        public HashedPassword hash(RawPassword password) {

            if (pauseBeforeNextHash.getAndSet(false)) {
                ContainerFailure.pause(IsolatedRedisContainerConfig.container());
            }

            return delegate.hash(password);
        }

        @Override
        public boolean matches(RawPassword rawPassword, HashedPassword hashedPassword) {

            return delegate.matches(rawPassword, hashedPassword);
        }

        public void pauseRedisBeforeNextHash() {

            pauseBeforeNextHash.set(true);
        }

        // A test that arms the switch and fails before spending it would otherwise break the next
        // test instead of itself.
        public void reset() {

            pauseBeforeNextHash.set(false);
        }
    }
}
