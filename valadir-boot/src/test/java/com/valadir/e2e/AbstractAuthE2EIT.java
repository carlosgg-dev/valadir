package com.valadir.e2e;

import com.valadir.e2e.support.CaptchaVerifierTestConfig;
import com.valadir.e2e.support.CaptchaVerifierTestConfig.ControllableCaptchaVerifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountActivationNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountLockedNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingPasswordResetNotifier;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.containers.PostgresContainerConfig;
import com.valadir.test.containers.RedisContainerConfig;
import com.valadir.web.config.ApiRoutes;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base for functional E2E tests over the full HTTP → UseCase → Adapter → Postgres/Redis stack.
 * Boots one shared context (real containers via {@code @ServiceConnection}); each test starts
 * from a clean slate — Redis flushed, tables emptied, capturing doubles reset.
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
public abstract class AbstractAuthE2EIT {

    protected static final String FULL_NAME = "Bruce Wayne";
    protected static final String GIVEN_NAME = "Batman";

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

    @BeforeEach
    void resetSharedState() {

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

    // --- Readers: pull one value out of a response or the test doubles.

    protected String accessTokenOf(Response response) {

        return requireToken(response, "accessToken");
    }

    protected String refreshTokenOf(Response response) {

        return requireToken(response, "refreshToken");
    }

    protected String activationOtpFor(String email) {

        return accountActivationNotifier.lastOtpFor(email)
            .orElseThrow(() -> new IllegalStateException("No activation OTP captured for " + email))
            .value();
    }

    protected String accountIdOf(String email) {

        return accountJpaRepository.findByEmail(email)
            .orElseThrow()
            .getId().toString();
    }

    protected List<String> userTokensOf(String accountId) {

        var tokens = redisTemplate.opsForSet().members(RedisKeySpace.forUserTokens(accountId));

        return tokens == null
            ? List.of()
            : List.copyOf(tokens);
    }

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

    private String requireToken(Response response, String field) {

        String token = response.path(field);

        if (token == null) {
            throw new IllegalStateException("No %s in response (status %s)".formatted(field, response.statusCode()));
        }

        return token;
    }
}
