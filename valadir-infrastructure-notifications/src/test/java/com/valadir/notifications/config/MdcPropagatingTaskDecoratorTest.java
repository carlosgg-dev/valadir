package com.valadir.notifications.config;

import com.valadir.common.mdc.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcPropagatingTaskDecoratorTest {

    private static final String REQUEST_ID = "5d3a1c88-2f4b";

    private final MdcPropagatingTaskDecorator decorator = new MdcPropagatingTaskDecorator();

    @AfterEach
    void tearDown() {

        MDC.clear();
    }

    @Test
    void decorate_submitterHasContext_runsTheTaskUnderItOnTheWorkerThread() throws Exception {

        MDC.put(MdcKeys.REQUEST_ID, REQUEST_ID);
        Map<String, String> contextInsideTask = new HashMap<>();

        runOnWorkerThread(decorator.decorate(() -> contextInsideTask.putAll(MDC.getCopyOfContextMap())));

        assertThat(contextInsideTask).containsEntry(MdcKeys.REQUEST_ID, REQUEST_ID);
    }

    @Test
    void decorate_taskFinishes_leavesTheWorkerThreadWithoutContext() throws Exception {

        MDC.put(MdcKeys.REQUEST_ID, REQUEST_ID);

        Map<String, String> contextLeftOnWorker = runOnWorkerThread(decorator.decorate(() -> {}));

        assertThat(contextLeftOnWorker).isNull();
    }

    @Test
    void decorate_submitterWithoutContext_stillRunsTheTask() throws Exception {

        var executed = new AtomicBoolean();

        runOnWorkerThread(decorator.decorate(() -> executed.set(true)));

        assertThat(executed).isTrue();
    }

    /**
     * @return the MDC the task left behind on the worker, which the pool would hand to the next one.
     */
    private static Map<String, String> runOnWorkerThread(Runnable task) throws InterruptedException {

        var contextLeftBehind = new AtomicReference<Map<String, String>>();

        var worker = new Thread(() -> {
            task.run();
            contextLeftBehind.set(MDC.getCopyOfContextMap());
        });

        worker.start();
        worker.join();

        return contextLeftBehind.get();
    }
}
