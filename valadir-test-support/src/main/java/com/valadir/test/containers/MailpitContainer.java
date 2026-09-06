package com.valadir.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * The same Mailpit the compose file runs, started by the test itself: an IT must never depend on a
 * mail server someone happened to leave running. Unlike Postgres and Redis it has no
 * {@code ServiceConnection} support, so the endpoints are read off the container directly.
 */
public final class MailpitContainer {

    private static final int SMTP_PORT = 1025;
    private static final int HTTP_PORT = 8025;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.21")
        .withExposedPorts(SMTP_PORT, HTTP_PORT)
        .waitingFor(Wait.forHttp("/").forPort(HTTP_PORT));

    static {
        MAILPIT.start();
    }

    private MailpitContainer() {

    }

    public static String host() {

        return MAILPIT.getHost();
    }

    public static int smtpPort() {

        return MAILPIT.getMappedPort(SMTP_PORT);
    }

    /** Where the delivered mail is read back from, as {@code http://host:port}. */
    public static String apiBaseUrl() {

        return "http://" + host() + ":" + MAILPIT.getMappedPort(HTTP_PORT);
    }
}
