package com.valadir.notifications.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Copies the submitter's logging context into the pool thread, so a notification failure is logged
 * against the request that caused it. {@link #decorate} runs on the request thread, where that
 * context still exists; the worker is left clean because a reused pool thread would otherwise label
 * the next notification with the wrong request.
 */
class MdcPropagatingTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable task) {

        Map<String, String> submitterContext = MDC.getCopyOfContextMap();

        return () -> {
            if (submitterContext != null) {
                MDC.setContextMap(submitterContext);
            }

            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
