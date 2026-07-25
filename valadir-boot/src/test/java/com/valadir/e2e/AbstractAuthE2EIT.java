package com.valadir.e2e;

import com.valadir.e2e.support.CaptchaVerifierTestConfig;
import com.valadir.e2e.support.CaptchaVerifierTestConfig.ControllableCaptchaVerifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountActivationNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingAccountLockedNotifier;
import com.valadir.e2e.support.NotifierCapturingTestConfig.CapturingPasswordResetNotifier;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
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

    protected void register(String email, String password) {

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "password", password,
                "fullName", "Bruce Wayne",
                "givenName", "Batman"
            ))
            .when()
            .post(ApiRoutes.Auth.Registration.REGISTER_PATH)
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    protected void registerAndActivate(String email, String password) {

        register(email, password);

        var otp = accountActivationNotifier.lastOtpFor(email)
            .orElseThrow(() -> new IllegalStateException("No activation OTP captured for " + email));

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", email,
                "code", otp.value()
            ))
            .when()
            .post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
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
}
