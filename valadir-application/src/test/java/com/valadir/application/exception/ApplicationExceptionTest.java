package com.valadir.application.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationExceptionTest {

    private static final String MESSAGE = "use case failed";
    private static final RuntimeException CAUSE = new RuntimeException("root cause");

    @Test
    void constructor_messageAndErrorCode_setsErrorCode() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
    }

    @Test
    void constructor_messageErrorCodeAndCause_preservesCause() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED, CAUSE);

        assertThat(exception.getCause()).isSameAs(CAUSE);
    }
}
