package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.e2e.support.CaptchaVerifierTestConfig.ControllableCaptchaVerifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountActivationNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountLockedNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingPasswordResetNotifier;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.security.redis.TokenFingerprint;
import com.valadir.web.config.ApiRoutes;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The vocabulary every HTTP-level test speaks: steps, readers, preconditions and the per-test reset.
 *
 * <p>Deliberately carries no class-level annotation. Which context a suite boots — the shared
 * containers of {@link AbstractAuthE2EIT} or the pausable ones of the resilience suite — is the
 * subclass's decision, and inheriting this vocabulary must not drag a container setup along with it.
 */
public abstract class AuthE2ESupport {

    protected static final String FULL_NAME = "Bruce Wayne";
    protected static final String GIVEN_NAME = "Batman";

    private static final int CONCURRENT_CALL_TIMEOUT_SECONDS = 30;

    @LocalServerPort
    private int port;

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected AccountJpaRepository accountJpaRepository;

    @Autowired
    protected UserJpaRepository userJpaRepository;

    @Autowired
    protected CapturingAccountActivationNotifier accountActivationNotifier;

    @Autowired
    protected CapturingPasswordResetNotifier passwordResetNotifier;

    @Autowired
    protected CapturingAccountLockedNotifier accountLockedNotifier;

    @Autowired
    protected ControllableCaptchaVerifier captchaVerifier;

    // protected, not package-private: the resilience suite extends this from another package, and a
    // lifecycle method that is not truly inherited would silently stop clearing state between tests.
    @BeforeEach
    protected void resetSharedState() {

        RestAssured.port = port;

        flushRedis();
        userJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        accountActivationNotifier.reset();
        passwordResetNotifier.reset();
        accountLockedNotifier.reset();
        captchaVerifier.reset();
    }

    private void flushRedis() {

        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());

        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    // --- Steps: one HTTP call each, no assertions. Asserting the outcome is the test's job, which
    // is what lets the same step drive both the success and the failure cases of a flow.

    protected Response register(String email, String password) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "password", password,
                "fullName", FULL_NAME,
                "givenName", GIVEN_NAME
            ))
            .when()
            .post(ApiRoutes.Auth.Registration.REGISTER_PATH);
    }

    protected Response registerWithoutEmail(String password) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "password", password,
                "fullName", FULL_NAME,
                "givenName", GIVEN_NAME
            ))
            .when()
            .post(ApiRoutes.Auth.Registration.REGISTER_PATH);
    }

    protected Response activate(String email, String code) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "code", code
            ))
            .when()
            .post(ApiRoutes.Auth.Registration.ACTIVATE_PATH);
    }

    protected Response resendActivationCode(String email) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email))
            .when()
            .post(ApiRoutes.Auth.Registration.RESEND_PATH);
    }

    protected Response login(String email, String password) {

        return login(email, password, null);
    }

    protected Response login(String email, String password, String captchaToken) {

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        if (captchaToken != null) {
            body.put("captchaToken", captchaToken);
        }

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ApiRoutes.Auth.Session.LOGIN_PATH);
    }

    // No Authorization header on purpose: refresh is a public POST route,
    // the refresh token in the body is the only credential.
    protected Response refresh(String refreshToken) {

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(ApiRoutes.Auth.Session.REFRESH_PATH);
    }

    // Anonymous call against a protected route: no Authorization header.
    protected Response logout(String refreshToken) {

        return logout(null, refreshToken);
    }

    protected Response logout(String accessToken, String refreshToken) {

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);

        var request = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body);

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .post(ApiRoutes.Auth.Session.LOGOUT_PATH);
    }

    // Anonymous call against a protected route: no Authorization header.
    protected Response logoutAll() {

        return logoutAll(null);
    }

    protected Response logoutAll(String accessToken) {

        var request = RestAssured.given();

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .post(ApiRoutes.Auth.Session.LOGOUT_ALL_PATH);
    }

    protected Response initiatePasswordReset(String email) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email))
            .when()
            .post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH);
    }

    protected Response verifyPasswordResetOtp(String email, String code) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "code", code
            ))
            .when()
            .post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH);
    }

    protected Response completePasswordReset(String verificationToken, String newPassword) {

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "verificationToken", verificationToken,
                "newPassword", newPassword
            ))
            .when()
            .post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH);
    }

    // --- End of steps

    // --- Readers: pull one value out of a response or the test doubles.

    protected String accessTokenOf(Response response) {

        return requireToken(response, "accessToken");
    }

    protected String refreshTokenOf(Response response) {

        return requireToken(response, "refreshToken");
    }

    protected List<String> sessionFingerprintsFor(String accountId) {

        var fingerprints = redisTemplate.opsForSet().members(RedisKeySpace.forUserTokens(accountId));

        return fingerprints == null
            ? List.of()
            : List.copyOf(fingerprints);
    }

    protected String fingerprintOf(String token) {

        return TokenFingerprint.of(token).value();
    }

    protected String refreshTokenKeyOf(String refreshToken) {

        return RedisKeySpace.forRefreshToken(TokenFingerprint.of(refreshToken));
    }

    protected String accountIdFor(String email) {

        return accountJpaRepository.findByEmail(email)
            .orElseThrow()
            .getId().toString();
    }

    protected String activationOtpFor(String email) {

        return accountActivationNotifier.lastOtpFor(email)
            .orElseThrow(() -> new IllegalStateException("No activation OTP captured for " + email))
            .value();
    }

    protected String passwordResetOtpFor(String email) {

        return passwordResetNotifier.lastOtpFor(email)
            .orElseThrow(() -> new IllegalStateException("No password reset OTP captured for " + email))
            .value();
    }

    protected String verificationTokenOf(Response response) {

        return requireToken(response, "verificationToken");
    }

    // Derived from the real code instead of drawn at random: it stays a well-formed 6-digit OTP
    // (a malformed one would trip Bean Validation first) and its first digit always differs,
    // so it can never collide with it.
    protected String otherOtpThan(String otp) {

        return otp.startsWith("1")
            ? "2" + otp.substring(1)
            : "1" + otp.substring(1);
    }

    protected static List<Response> responsesWithStatus(List<Response> responses, HttpStatus status) {

        return responses.stream()
            .filter(response -> response.statusCode() == status.value())
            .toList();
    }

    private String requireToken(Response response, String field) {

        String token = response.path(field);

        if (token == null) {
            throw new IllegalStateException("No %s in response (status %s)".formatted(field, response.statusCode()));
        }

        return token;
    }

    // --- End of readers

    // --- Preconditions: composed on the steps above, and they do assert, because a broken
    // precondition must fail on the spot instead of surfacing as a null token further down.

    protected void registerAndActivate(String email, String password) {

        register(email, password)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String activationOtp = activationOtpFor(email);

        activate(email, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    // --- End of preconditions

    // --- Assertions shared by every suite that drives a dependency into failure.

    /**
     * Nothing but the error code crosses the boundary. An outage must be externally
     * indistinguishable from any other 503 — no stack trace, no host, no driver wording.
     */
    protected void assertOpaqueInfrastructureFailure(Response response) {

        response.then().statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());

        assertThat(response.jsonPath().getMap("$"))
            .containsExactly(Map.entry("code", ErrorCode.INFRASTRUCTURE_UNAVAILABLE.getCode()));
    }

    // --- End of assertions

    // --- Clock: crossing a tick, for the guarantees that are expressed in whole seconds.

    /**
     * A JWT carries {@code iat} in whole seconds, so a token minted within the same second as a
     * revocation cutoff cannot be proven newer than it and is denied. Crossing the tick is what
     * makes a test about signing in again measure the cutoff instead of that ambiguity.
     */
    protected static void awaitTheNextSecond() {

        long secondOfTheRevocation = Instant.now().getEpochSecond();

        Awaitility.await("the clock to cross into the second after the revocation")
            .atMost(Duration.ofSeconds(2))
            .pollInterval(Duration.ofMillis(20))
            .until(() -> Instant.now().getEpochSecond() > secondOfTheRevocation);
    }

    // --- End of clock

    // --- Concurrency: the only way to exercise an atomicity guarantee over HTTP.

    /**
     * Runs {@code call} {@code times} over as many threads, all released together by a latch.
     * Submitted one by one the calls would never overlap and the race under test would not happen.
     */
    protected List<Response> concurrently(int times, Callable<Response> call) {

        return concurrently(Collections.nCopies(times, call));
    }

    /**
     * The same burst over calls that differ, for a race between two sessions rather than two
     * attempts at one. Responses come back in the order the calls were given.
     */
    protected List<Response> concurrently(List<Callable<Response>> calls) {

        var startLine = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(calls.size())) {

            // Collected before awaiting any: draining in the same pipeline would serialize the burst
            List<Future<Response>> pending = calls.stream()
                .map(call -> executor.submit(() -> awaitThenCall(startLine, call)))
                .toList();

            startLine.countDown();

            return pending.stream()
                .map(AuthE2ESupport::completed)
                .toList();
        }
    }

    private static Response awaitThenCall(CountDownLatch startLine, Callable<Response> call) throws Exception {

        startLine.await();
        return call.call();
    }

    // A stream cannot carry checked exceptions, and a call that never returns is a broken burst
    // rather than a failed assertion.
    private static Response completed(Future<Response> pending) {

        try {
            return pending.get(CONCURRENT_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting a concurrent call", e);

        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("A concurrent call did not complete", e);
        }
    }

    // --- End of concurrency
}
