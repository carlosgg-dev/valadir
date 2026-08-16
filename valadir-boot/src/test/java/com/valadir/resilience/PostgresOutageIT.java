package com.valadir.resilience;

import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Fail-closed when Postgres cannot answer: credentials are unverifiable, so no token is issued, and a
 * write interrupted mid-flow leaves nothing half-done.
 *
 * <p>Unlike the Redis cases, these reach the controller — no filter touches Postgres — so the 503 comes
 * from {@code GlobalExceptionHandler} and not from {@code InfrastructureFailureFilter}.
 */
class PostgresOutageIT extends AbstractResilienceIT {

    private static final String EMAIL = "bruce@wayne.com";
    private static final String PASSWORD = PasswordMother.raw().value();

    @Test
    void login_postgresDown_deniesWithOpaqueInfrastructureError() {

        registerAndActivate(EMAIL, PASSWORD);

        pausePostgres();

        // Redis is alive, so the attempt counter lets the login through and the account lookup is the
        // first failing call. An empty body is also what pins "never issue a token".
        assertOpaqueInfrastructureFailure(login(EMAIL, PASSWORD));
    }

    // The denial must be temporary. Postgres has no circuit breaker, so what recovers here is the
    // connection pool after the socket timeout killed its connections — a pool left poisoned would
    // keep failing, and nothing else in the suite would notice.
    @Test
    void login_afterPostgresComesBack_succeedsAgain() {

        registerAndActivate(EMAIL, PASSWORD);

        pausePostgres();

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());

        resumePostgres();

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void activateAccount_postgresDownWhileWriting_deniesWithoutActivatingTheAccount() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String activationOtp = activationOtpFor(EMAIL);

        // Verifying the OTP is the last step before the write, so pausing there is what makes the
        // write the failing call. Pausing before the request would fail on the account lookup
        // instead, and the transactional write — the one the adapter's catch may not see — would
        // never run.
        otpHasher.pauseDatabaseBeforeNextMatch();

        assertOpaqueInfrastructureFailure(activate(EMAIL, activationOtp));

        resumePostgres();

        // The UPDATE is all-or-nothing: had it landed, the account would no longer be pending and the
        // same code would come back rejected. The OTP survives because its Redis cleanup runs after
        // the write and is never reached.
        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }
}
