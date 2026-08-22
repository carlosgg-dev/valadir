package com.valadir.config;

import com.valadir.config.LoginLockoutProperties.ThresholdProperties;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Rule;
import com.valadir.web.config.RateLimitProperties.Strategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the production YAML actually produces: a misspelled key does not fail, it falls back to the
 * library default in silence. Binds the real file with Spring Boot's own {@link Binder} and no
 * context, so nothing has to be overridden to make it run and the whole file stays assertable —
 * placeholders included, left unresolved on purpose.
 *
 * <p>The OTP TTLs and the JWT TTLs are left to the ITs, which already pin them against production:
 * one guardian per number. The lockout tiers are not — the ITs reach the first two, and reaching the
 * third costs fifteen logins, so what binds is asserted here and what it does is asserted there.
 */
class ProductionConfigurationTest {

    private static final Resource PRODUCTION = new ClassPathResource("application.yml");
    private static final Resource BOOT_TEST_CONFIGURATION = new ClassPathResource("application-test.yml");

    // By path because they belong to other modules: test resources are not published, so they never
    // reach this classpath.
    private static final List<Resource> TEST_CONFIGURATIONS = List.of(
        BOOT_TEST_CONFIGURATION,
        new FileSystemResource("../valadir-infrastructure-security/src/test/resources/application-test.yml"),
        new FileSystemResource("../valadir-infrastructure-persistence/src/test/resources/application.yml"));

    private static final Binder PRODUCTION_CONFIGURATION = binderOver(PRODUCTION);

    private static final Duration ONE_MINUTE = Duration.ofSeconds(60);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Test
    void rateLimitRules_productionConfiguration_bindTheFourteenIntendedBuckets() {

        var rateLimit = PRODUCTION_CONFIGURATION.bind("rate-limit", RateLimitProperties.class).get();

        assertThat(rateLimit.enabled()).isTrue();

        // Ordered, because RateLimitFilter evaluates in order and stops at the first rule that denies.
        assertThat(rateLimit.rules()).containsExactly(
            new Rule("/api/auth/login", Strategy.IP, 10, ONE_MINUTE),

            new Rule("/api/auth/register", Strategy.IP, 5, ONE_HOUR),
            new Rule("/api/auth/register", Strategy.EMAIL, 3, ONE_HOUR),

            new Rule("/api/auth/account-activation", Strategy.IP, 10, ONE_MINUTE),
            new Rule("/api/auth/account-activation", Strategy.EMAIL, 5, OTP_TTL),
            new Rule("/api/auth/account-activation/resend", Strategy.IP, 5, ONE_HOUR),
            new Rule("/api/auth/account-activation/resend", Strategy.EMAIL, 3, ONE_HOUR),

            new Rule("/api/auth/password-reset/initiate", Strategy.IP, 5, ONE_HOUR),
            new Rule("/api/auth/password-reset/initiate", Strategy.EMAIL, 3, ONE_HOUR),
            new Rule("/api/auth/password-reset/verify", Strategy.IP, 10, OTP_TTL),
            new Rule("/api/auth/password-reset/verify", Strategy.EMAIL, 5, OTP_TTL),
            new Rule("/api/auth/password-reset/complete", Strategy.IP, 5, OTP_TTL),

            new Rule("/api/auth/refresh", Strategy.IP, 30, ONE_MINUTE),

            new Rule("/api/**", Strategy.USER, 100, ONE_MINUTE)
        );
    }

    @Test
    void lockoutTiers_productionConfiguration_bindTheThreeEscalatingTiers() {

        var lockout = PRODUCTION_CONFIGURATION.bind("auth.lockout", LoginLockoutProperties.class).get();

        assertThat(lockout.window()).isEqualTo(ONE_HOUR);
        assertThat(lockout.challengeThreshold()).isEqualTo(3);

        // Unordered on purpose: LoginLockoutPolicy.lockoutFor takes the longest lockout among the
        // tiers reached, so the order in the file is legibility, not a guarantee.
        assertThat(lockout.thresholds()).containsExactlyInAnyOrder(
            new ThresholdProperties(5, Duration.ofSeconds(60)),
            new ThresholdProperties(10, Duration.ofMinutes(5)),
            new ThresholdProperties(15, Duration.ofMinutes(30)));
    }

    @Test
    void circuitBreakers_productionConfiguration_declareBothInstancesWithTheirThresholds() {

        // Exactly these two names: registry.circuitBreaker("redis") does not fail on a renamed
        // instance, it hands back a breaker with the library defaults instead of these thresholds.
        assertThat(circuitBreakerInstances())
            .containsOnlyKeys("redis", "captcha")
            .containsEntry("redis", new BreakerConfig("COUNT_BASED", 20, 10, 50, Duration.ofSeconds(5), 3))
            .containsEntry("captcha", new BreakerConfig("COUNT_BASED", 10, 5, 50, Duration.ofSeconds(30), 2));
    }

    @Test
    void redisBreaker_productionConfiguration_recordsOnlyAFailureToReachTheDependency() {

        // Narrowing it to a subclass would leave genuine outages off the circuit. That the name still
        // resolves is not asserted here: a class that moved fails the binding of every IT context.
        assertThat(recordedExceptionsOf("redis"))
            .containsExactly("org.springframework.dao.DataAccessException");
    }

    @Test
    void captchaBreaker_productionConfiguration_doesNotRecordAnInterpretableRejection() {

        // A 4xx from Turnstile is an answer, not an outage: counting it towards the circuit would
        // turn an interpretable rejection into the fail-open verdict of a provider outage.
        assertThat(recordedExceptionsOf("captcha"))
            .containsExactly(
                "org.springframework.web.client.ResourceAccessException",
                "org.springframework.web.client.HttpServerErrorException")
            .doesNotContain("org.springframework.web.client.HttpClientErrorException");
    }

    @Test
    void outboundDeadlines_productionConfiguration_areBoundOnEveryDependency() {

        // Every outbound call this system makes. Misspell one of these keys and the driver's own
        // default silently takes its place, which for some of them is no deadline at all.
        assertThat(duration("spring.data.redis.timeout")).isEqualTo(Duration.ofSeconds(2));
        assertThat(duration("spring.data.redis.connect-timeout")).isEqualTo(Duration.ofSeconds(1));

        assertThat(duration("auth.captcha.connect-timeout")).isEqualTo(Duration.ofSeconds(2));
        assertThat(duration("auth.captcha.read-timeout")).isEqualTo(Duration.ofSeconds(3));

        // Raw numbers: Hikari takes milliseconds and pgjdbc seconds, so the unit lives in the key.
        assertThat(integer("spring.datasource.hikari.connection-timeout")).isEqualTo(2000);

        // Pass-through bags of driver properties: their keys never reach a typed field where a typo
        // would be noticed, so they are asserted entry by entry.
        assertThat(entries("spring.datasource.hikari.data-source-properties"))
            .containsEntry("socketTimeout", "5");

        assertThat(entries("spring.mail.properties"))
            .containsEntry("mail.smtp.connectiontimeout", "2000")
            .containsEntry("mail.smtp.timeout", "5000")
            .containsEntry("mail.smtp.writetimeout", "5000");
    }

    @Test
    void keys_productionConfiguration_areExactlyTheDeclaredOnes() {

        // Asserting values only protects the keys they name. This protects the rest: a typo is not a
        // changed value, it is a key that stops existing while a stranger appears beside it.
        assertThat(keysOf(PRODUCTION)).containsExactlyInAnyOrder(
            "server.port",
            "spring.application.name",
            "spring.jackson.default-property-inclusion",
            "spring.jackson.serialization.write-dates-as-timestamps",

            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.datasource.driver-class-name",
            "spring.datasource.hikari.connection-timeout",
            "spring.datasource.hikari.data-source-properties.socketTimeout",

            "spring.jpa.hibernate.ddl-auto",
            "spring.jpa.properties.hibernate.dialect",
            "spring.jpa.show-sql",
            "spring.jpa.open-in-view",

            "spring.data.redis.host",
            "spring.data.redis.port",
            "spring.data.redis.password",
            "spring.data.redis.timeout",
            "spring.data.redis.connect-timeout",

            "spring.mail.host",
            "spring.mail.port",
            "spring.mail.properties.mail.smtp.auth",
            "spring.mail.properties.mail.smtp.starttls.enable",
            "spring.mail.properties.mail.smtp.connectiontimeout",
            "spring.mail.properties.mail.smtp.timeout",
            "spring.mail.properties.mail.smtp.writetimeout",

            "notifications.mail.from",
            "notifications.async.core-pool-size",
            "notifications.async.max-pool-size",
            "notifications.async.queue-capacity",

            "auth.account-activation.otp.ttl",
            "auth.password-reset.otp.ttl",
            "auth.password-reset.verification-token.ttl",
            "auth.lockout.window",
            "auth.lockout.challenge-threshold",
            "auth.lockout.thresholds[].min-failures",
            "auth.lockout.thresholds[].lockout",
            "auth.captcha.enabled",
            "auth.captcha.verify-url",
            "auth.captcha.secret",
            "auth.captcha.connect-timeout",
            "auth.captcha.read-timeout",
            "auth.jwt.private-key",
            "auth.jwt.access-token-ttl",
            "auth.jwt.refresh-token-ttl",

            "resilience4j.circuitbreaker.instances.redis.sliding-window-type",
            "resilience4j.circuitbreaker.instances.redis.sliding-window-size",
            "resilience4j.circuitbreaker.instances.redis.minimum-number-of-calls",
            "resilience4j.circuitbreaker.instances.redis.failure-rate-threshold",
            "resilience4j.circuitbreaker.instances.redis.wait-duration-in-open-state",
            "resilience4j.circuitbreaker.instances.redis.permitted-number-of-calls-in-half-open-state",
            "resilience4j.circuitbreaker.instances.redis.record-exceptions[]",
            "resilience4j.circuitbreaker.instances.captcha.sliding-window-type",
            "resilience4j.circuitbreaker.instances.captcha.sliding-window-size",
            "resilience4j.circuitbreaker.instances.captcha.minimum-number-of-calls",
            "resilience4j.circuitbreaker.instances.captcha.failure-rate-threshold",
            "resilience4j.circuitbreaker.instances.captcha.wait-duration-in-open-state",
            "resilience4j.circuitbreaker.instances.captcha.permitted-number-of-calls-in-half-open-state",
            "resilience4j.circuitbreaker.instances.captcha.record-exceptions[]",

            "scheduler.pending-activation-account.grace-period",
            "scheduler.pending-activation-account.purge-cron",

            "rate-limit.enabled",
            "rate-limit.rules[].path",
            "rate-limit.rules[].strategy",
            "rate-limit.rules[].max-requests",
            "rate-limit.rules[].window"
        );
    }

    @Test
    void keys_everyTestConfiguration_existInProduction() {

        // A test file may only override something that is really there. A key existing nowhere else
        // is a typo or a wrong nesting level, and both are invisible: the override does nothing and
        // the test keeps running against the production value it meant to replace.
        var productionKeys = keysOf(PRODUCTION);

        TEST_CONFIGURATIONS.forEach(configuration ->
                                        assertThat(keysOf(configuration))
                                            .describedAs(configuration.getDescription())
                                            .isSubsetOf(productionKeys));
    }

    @Test
    void values_bootTestConfiguration_neverRepeatTheProductionOne() {

        // An override earns its place only by differing: repeating the production value leaves the
        // ITs measuring this file while believing they pin production, which is how the JWT TTLs
        // went unnoticed. Only this file, the one layered on top of the production YAML — the other
        // modules do not have it on their classpath, so what they declare is the only value there is.
        var production = propertiesOf(PRODUCTION);

        var redundant = propertiesOf(BOOT_TEST_CONFIGURATION).entrySet().stream()
            .filter(override -> override.getValue().equals(production.get(override.getKey())))
            .map(Map.Entry::getKey)
            .toList();

        assertThat(redundant).isEmpty();
    }

    @Test
    void secrets_productionConfiguration_comeFromTheEnvironment() {

        // Guards the one mistake a revert cannot undo: pasting a real secret into a versioned file.
        assertThat(string("auth.jwt.private-key")).isEqualTo("${JWT_PRIVATE_KEY}");
        assertThat(string("auth.captcha.secret")).isEqualTo("${TURNSTILE_SECRET}");
        assertThat(string("spring.datasource.password")).isEqualTo("${DATABASE_PASSWORD}");
        assertThat(string("spring.data.redis.password")).isEqualTo("${REDIS_PASSWORD}");
    }

    private static Binder binderOver(Resource resource) {

        return new Binder(ConfigurationPropertySources.from(sourcesOf(resource)));
    }

    private static Map<String, BreakerConfig> circuitBreakerInstances() {

        return PRODUCTION_CONFIGURATION
            .bind("resilience4j.circuitbreaker.instances", Bindable.mapOf(String.class, BreakerConfig.class))
            .get();
    }

    private static List<String> recordedExceptionsOf(String instance) {

        return PRODUCTION_CONFIGURATION
            .bind("resilience4j.circuitbreaker.instances." + instance + ".record-exceptions", Bindable.listOf(String.class))
            .get();
    }

    private static Duration duration(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, Duration.class).get();
    }

    private static Integer integer(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, Integer.class).get();
    }

    private static String string(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, String.class).get();
    }

    private static Map<String, String> entries(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, Bindable.mapOf(String.class, String.class)).get();
    }

    private static Set<String> keysOf(Resource resource) {

        return propertiesOf(resource).keySet();
    }

    private static Map<String, Object> propertiesOf(Resource resource) {

        Map<String, Object> entries = new TreeMap<>();

        for (PropertySource<?> source : sourcesOf(resource)) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    entries.put(collapseIndexes(name), enumerable.getProperty(name));
                }
            }
        }

        return entries;
    }

    private static List<PropertySource<?>> sourcesOf(Resource resource) {

        try {
            return new YamlPropertySourceLoader().load(resource.getFilename(), resource);

        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }

    private static String collapseIndexes(String name) {

        return name.replaceAll("\\[\\d+]", "[]");
    }

    record BreakerConfig(
        String slidingWindowType,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        int failureRateThreshold,
        Duration waitDurationInOpenState,
        int permittedNumberOfCallsInHalfOpenState) {

    }
}
