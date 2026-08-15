package com.valadir.resilience.support;

import com.github.dockerjava.api.DockerClient;
import org.awaitility.Awaitility;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * Takes a container away and gives it back.
 *
 * <p>{@code docker pause} freezes the process without closing its sockets, so the client hangs
 * instead of getting a clean refusal — the failure the timeouts exist to survive. A stopped
 * container would answer "connection refused" instantly and never exercise them.
 */
public final class ContainerFailure {

    private static final Duration READY_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration READY_POLL_INTERVAL = Duration.ofMillis(200);

    /**
     * Testcontainers' JVM-wide client. It must <b>not</b> be closed: the wrapper rejects it
     * ({@code "You should never close the global DockerClient!"}) and closing it would take down
     * every container in the fork. The unclosed-{@code Closeable} warning stands on purpose —
     * obeying it turns a passing suite into an {@code IllegalStateException} on the first pause.
     */
    private static final DockerClient DOCKER = DockerClientFactory.instance().client();

    private ContainerFailure() {

    }

    public static void pause(GenericContainer<?> container) {

        if (!isPaused(container)) {
            DOCKER.pauseContainerCmd(container.getContainerId()).exec();
        }
    }

    /**
     * Resumes and waits until the container answers again: returning on the unpause command alone
     * would leave the next test's cleanup racing a driver that has not reconnected.
     */
    public static void resume(GenericContainer<?> container, Runnable probe) {

        if (isPaused(container)) {
            DOCKER.unpauseContainerCmd(container.getContainerId()).exec();
        }

        // Until it is back the probe throws, not returns false — swallowing that is what polls.
        Awaitility.await("%s to answer after resuming".formatted(container.getDockerImageName()))
            .atMost(READY_TIMEOUT)
            .pollInterval(READY_POLL_INTERVAL)
            .ignoreExceptions()
            .until(() -> {
                probe.run();
                return true;
            });
    }

    private static boolean isPaused(GenericContainer<?> container) {

        var state = DOCKER.inspectContainerCmd(container.getContainerId())
            .exec()
            .getState();

        return Boolean.TRUE.equals(state.getPaused());
    }
}
