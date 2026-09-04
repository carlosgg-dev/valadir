package com.valadir.notifications.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final AsyncProperties properties;

    AsyncConfig(AsyncProperties properties) {

        this.properties = properties;
    }

    @Override
    public Executor getAsyncExecutor() {

        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("notif-async-");
        executor.setTaskDecorator(new MdcPropagatingTaskDecorator());
        // On saturation, drop the notification instead of letting AbortPolicy turn a login into a 500.
        // Failing open is correct under the brute-force scenario.
        executor.setRejectedExecutionHandler(
            (task, threadPoolExecutor) -> log.warn("Account-locked notification dropped: async pool saturated")
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // In a clean shutdown, without this limit, a blocked SMTP connection
        // could indefinitely prevent the application from closing
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {

        // Async void methods cannot surface failures to a caller.
        // Without a handler the exception is logged by Spring's default and lost.
        // We keep it explicit at WARN since these are best-effort notifications, not request-critical failures.
        return (throwable, method, params) -> log.warn("Async notification '{}' failed", method.getName(), throwable);
    }
}
