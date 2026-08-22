package com.valadir.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CaptchaPropertiesTest {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String SECRET = "a-turnstile-secret";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Test
    void constructor_enabledWithVerifyUrlAndSecret_isAllowed() {

        assertThatNoException()
            .isThrownBy(() -> new CaptchaProperties(VERIFY_URL, SECRET, CONNECT_TIMEOUT, READ_TIMEOUT, true));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void constructor_enabledWithoutVerifyUrl_throws(String verifyUrl) {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new CaptchaProperties(verifyUrl, SECRET, CONNECT_TIMEOUT, READ_TIMEOUT, true));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void constructor_enabledWithoutSecret_throws(String secret) {

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new CaptchaProperties(VERIFY_URL, secret, CONNECT_TIMEOUT, READ_TIMEOUT, true));
    }

    @Test
    void constructor_disabledWithVerifyUrlAndSecret_isAllowed() {

        assertThatNoException()
            .isThrownBy(() -> new CaptchaProperties(VERIFY_URL, SECRET, CONNECT_TIMEOUT, READ_TIMEOUT, false));
    }

    @Test
    void constructor_disabledWithoutVerifyUrl_isAllowed() {

        assertThatNoException()
            .isThrownBy(() -> new CaptchaProperties(null, SECRET, CONNECT_TIMEOUT, READ_TIMEOUT, false));
    }

    @Test
    void constructor_disabledWithoutSecret_isAllowed() {

        assertThatNoException()
            .isThrownBy(() -> new CaptchaProperties(VERIFY_URL, null, CONNECT_TIMEOUT, READ_TIMEOUT, false));
    }

    @Test
    void validation_disabledWithoutTimeouts_stillRejectsBoth() {

        var properties = new CaptchaProperties(VERIFY_URL, SECRET, null, null, false);

        assertThat(violationsOf(properties))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder("connectTimeout", "readTimeout");
    }

    // The @NotNull constraints never run on `new`, only through a Validator — which is what Spring
    // applies while binding. Without one, a test could only see the constructor half of the record.
    private static Set<ConstraintViolation<CaptchaProperties>> violationsOf(CaptchaProperties properties) {

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(properties);
        }
    }
}
