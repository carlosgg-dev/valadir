package com.valadir.notifications.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AsyncPropertiesTest {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 50;

    @Test
    void constructor_corePoolSizeBelowOne_throws() {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new AsyncProperties(0, MAX_POOL_SIZE, QUEUE_CAPACITY));
    }

    @Test
    void constructor_maxPoolSizeBelowCorePoolSize_throws() {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new AsyncProperties(CORE_POOL_SIZE, CORE_POOL_SIZE - 1, QUEUE_CAPACITY));
    }

    @Test
    void constructor_negativeQueueCapacity_throws() {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new AsyncProperties(CORE_POOL_SIZE, MAX_POOL_SIZE, -1));
    }
}
