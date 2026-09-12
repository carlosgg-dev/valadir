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

import static java.util.stream.Collectors.toMap;
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
    private static final Resource SECURITY_TEST_CONFIGURATION =
        new FileSystemResource("../valadir-infrastructure-security/src/test/resources/application-test.yml");

    private static final List<Resource> TEST_CONFIGURATIONS = List.of(
        BOOT_TEST_CONFIGURATION,
        SECURITY_TEST_CONFIGURATION,
        new FileSystemResource("../valadir-infrastructure-persistence/src/test/resources/application.yml"));

    private static final Binder PRODUCTION_CONFIGURATION = binderOver(PRODUCTION);

    private static final Duration ONE_MINUTE = Duration.ofSeconds(60);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Test
    void rateLimitRules_productionConfiguration_bindEveryIntendedBucketInOrder() {

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
        assertThat(recordedExceptionsFor("redis"))
            .containsExactly("org.springframework.dao.DataAccessException");
    }

    @Test
    void captchaBreaker_productionConfiguration_doesNotRecordAnInterpretableRejection() {

        // A 4xx from Turnstile is an answer, not an outage: counting it towards the circuit would
        // turn an interpretable rejection into the fail-open verdict of a provider outage.
        assertThat(recordedExceptionsFor("captcha"))
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
    void harmfulFrameworkDefaults_productionConfiguration_areEachOverridden() {

        // Only the keys whose absence is not an error but an answer we do not want, and the binding
        // below fails the moment one of them is misspelled. The rest of the file needs no second
        // guardian: a typo under a namespace asserted above breaks that assertion, and a typo in a key
        // read through @Value leaves a placeholder unresolved, so no context starts at all.

        // Defaults to true, announced only as a log line: lazy loading keeps working in the web layer,
        // so an N+1 introduced behind a view renders correctly here and degrades in production.
        assertThat(bool("spring.jpa.open-in-view")).isFalse();

        // Defaults to none against a real database: the schema silently stops following the entities.
        assertThat(string("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");

        // Defaults to always: every null field reappears in every response body.
        assertThat(string("spring.jackson.default-property-inclusion")).isEqualTo("non_null");

        // Defaults to true: instants serialize as epoch numbers instead of ISO-8601.
        assertThat(bool("spring.jackson.serialization.write-dates-as-timestamps")).isFalse();
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
        // went unnoticed. Only this file, the one layered on top of the production YAML: a module
        // without it on its classpath declares the only value there is, so repetition there is the
        // requirement rather than the defect, and the rule below is its mirror image.
        var production = propertiesOf(PRODUCTION);

        var redundant = propertiesOf(BOOT_TEST_CONFIGURATION).entrySet().stream()
            .filter(override -> override.getValue().equals(production.get(override.getKey())))
            .map(Map.Entry::getKey)
            .toList();

        assertThat(redundant).isEmpty();
    }

    @Test
    void jwtTtls_securityTestConfiguration_repeatTheProductionOnes() {

        // The opposite requirement to the rule above, for the opposite reason: this module has no
        // production YAML on its classpath, so its tests can only measure what it declares itself.
        // Left untied, a TTL changed in production leaves them green against the old number — the
        // same miss the rule above exists for, one module over. The private key is not tied: a test
        // key that equalled the production one would be a leaked secret, not a pinned value.
        var production = propertiesOf(PRODUCTION);

        assertThat(propertiesOf(SECURITY_TEST_CONFIGURATION))
            .containsEntry("auth.jwt.access-token-ttl", production.get("auth.jwt.access-token-ttl"))
            .containsEntry("auth.jwt.refresh-token-ttl", production.get("auth.jwt.refresh-token-ttl"));
    }

    @Test
    void placeholders_productionConfiguration_areExactlyTheOnesTheEnvironmentSupplies() {

        // Guards the mistake a revert cannot undo — a real secret pasted into a versioned file
        // drops its key from this map — and says which value each variable feeds. The two that
        // are not secrets belong here too: what this pins is not the four that must stay hidden,
        // it is the six a deployment has to supply.
        assertThat(placeholdersOf(PRODUCTION)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "spring.datasource.url", "DATABASE_URL",
            "spring.datasource.username", "DATABASE_USER",
            "spring.datasource.password", "DATABASE_PASSWORD",
            "spring.data.redis.password", "REDIS_PASSWORD",
            "auth.captcha.secret", "TURNSTILE_SECRET",
            "auth.jwt.private-key", "JWT_PRIVATE_KEY"));
    }

    private static Binder binderOver(Resource resource) {

        return new Binder(ConfigurationPropertySources.from(sourcesOf(resource)));
    }

    private static Map<String, BreakerConfig> circuitBreakerInstances() {

        return PRODUCTION_CONFIGURATION
            .bind("resilience4j.circuitbreaker.instances", Bindable.mapOf(String.class, BreakerConfig.class))
            .get();
    }

    private static List<String> recordedExceptionsFor(String instance) {

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

    private static Boolean bool(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, Boolean.class).get();
    }

    private static Map<String, String> entries(String key) {

        return PRODUCTION_CONFIGURATION.bind(key, Bindable.mapOf(String.class, String.class)).get();
    }

    /** Each key that reads a placeholder, mapped to the variable name behind it. */
    private static Map<String, String> placeholdersOf(Resource resource) {

        return propertiesOf(resource).entrySet().stream()
            .filter(entry -> entry.getValue().toString().startsWith("${"))
            .collect(toMap(Map.Entry::getKey, entry -> variableIn(entry.getValue().toString())));
    }

    private static String variableIn(String placeholder) {

        return placeholder.substring("${".length(), placeholder.length() - "}".length());
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
