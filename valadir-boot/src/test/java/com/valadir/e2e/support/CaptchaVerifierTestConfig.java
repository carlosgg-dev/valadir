package com.valadir.e2e.support;

import com.valadir.application.port.out.CaptchaVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replaces the Turnstile adapter with a controllable double: E2E must never call the real
 * Cloudflare endpoint (external, flaky, not containerizable). Defaults to accepting every
 * token; individual tests flip it to assert the 403 SEC-008 contract.
 */
@TestConfiguration(proxyBeanMethods = false)
public class CaptchaVerifierTestConfig {

    @Bean
    @Primary
    ControllableCaptchaVerifier controllableCaptchaVerifier() {

        return new ControllableCaptchaVerifier();
    }

    public static class ControllableCaptchaVerifier implements CaptchaVerifier {

        private final AtomicBoolean valid = new AtomicBoolean(true);

        @Override
        public boolean isValid(String token) {

            // Mirrors the real adapter's contract: an absent token is never valid.
            // Without this guard the step-up 403 would be untestable.
            if (token == null || token.isBlank()) {
                return false;
            }

            return valid.get();
        }

        public void rejectAllTokens() {

            valid.set(false);
        }

        public void reset() {

            valid.set(true);
        }
    }
}
