package com.valadir.notifications.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class NotificationsPropertiesTest {

    private static final String FROM_ADDRESS = "noreply@valadir.local";

    @Test
    void constructor_validFromAddress_doesNotThrow() {

        assertThatNoException().isThrownBy(() -> new NotificationsProperties(FROM_ADDRESS));
    }

    // Configuration, so it must stop the deployment from starting rather than answer a request:
    // a mail with no sender is refused by the SMTP server, one send at a time, forever.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_missingFromAddress_throws(String fromAddress) {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new NotificationsProperties(fromAddress));
    }
}
