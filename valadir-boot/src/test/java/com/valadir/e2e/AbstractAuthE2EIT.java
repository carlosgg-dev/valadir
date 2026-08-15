package com.valadir.e2e;

import com.valadir.e2e.support.CaptchaVerifierTestConfig;
import com.valadir.e2e.support.NotifierCapturingTestConfig;
import com.valadir.test.containers.PostgresContainerConfig;
import com.valadir.test.containers.RedisContainerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base for functional E2E tests. Boots one shared context over the shared containers
 * (via {@code @ServiceConnection}); {@link AuthE2ESupport} provides the vocabulary and clears the
 * state before each test.
 *
 * <p>The per-IP rate limit is off here: a burst from localhost would answer 429 before the flow
 * under test ever ran. Enforcement is owned by {@code RateLimitEnforcementIT}, which turns it back on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "rate-limit.enabled=false")
@Import({
    PostgresContainerConfig.class,
    RedisContainerConfig.class,
    NotifierCapturingTestConfig.class,
    CaptchaVerifierTestConfig.class
})
public abstract class AbstractAuthE2EIT extends AuthE2ESupport {

}
