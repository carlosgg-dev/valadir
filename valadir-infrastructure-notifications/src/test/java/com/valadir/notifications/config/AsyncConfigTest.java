package com.valadir.notifications.config;

import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AsyncConfigTest {

    private static final String REQUEST_ID = "5d3a1c88-2f4b";
    private static final int TASK_TIMEOUT_SECONDS = 5;

    // One worker and no queue: a single occupied thread is enough to saturate the pool.
    private static final AsyncProperties SINGLE_THREADED = new AsyncProperties(1, 1, 0);

    private final AsyncConfig config = new AsyncConfig(SINGLE_THREADED);

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {

        executor = (ThreadPoolTaskExecutor) config.getAsyncExecutor();
    }

    @AfterEach
    void tearDown() {

        MDC.clear();
        executor.shutdown();
    }

    @Test
    void getAsyncExecutor_poolSaturated_dropsTheTaskInsteadOfRejectingTheCaller() throws Exception {

        var occupied = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var droppedTaskRan = new AtomicBoolean();

        executor.execute(() -> {
            occupied.countDown();
            awaitQuietly(release);
        });
        assertThat(occupied.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // Under AbortPolicy this throws TaskRejectedException, and the login that triggered the
        // notification answers 500 instead of its own verdict.
        assertThatNoException().isThrownBy(() -> executor.execute(() -> droppedTaskRan.set(true)));

        assertThat(droppedTaskRan).isFalse();
        release.countDown();
    }

    @Test
    void getAsyncExecutor_submittedTask_runsUnderTheSubmittersMdc() throws Exception {

        MDC.put(MdcKeys.REQUEST_ID, REQUEST_ID);
        var contextInsideTask = new AtomicReference<Map<String, String>>();
        var finished = new CountDownLatch(1);

        executor.execute(() -> {
            contextInsideTask.set(MDC.getCopyOfContextMap());
            finished.countDown();
        });

        assertThat(finished.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(contextInsideTask.get()).containsEntry(MdcKeys.REQUEST_ID, REQUEST_ID);
    }

    @Test
    void getAsyncUncaughtExceptionHandler_failedNotification_doesNotPropagate() throws Exception {

        var notification = AccountLockedNotifier.class
            .getMethod("notifyAccountLocked", Email.class, Duration.class, Language.class);

        assertThatNoException().isThrownBy(() -> Objects.requireNonNull(config.getAsyncUncaughtExceptionHandler())
            .handleUncaughtException(new IllegalStateException("notification failed"), notification));
    }

    private static void awaitQuietly(CountDownLatch latch) {

        try {
            latch.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
