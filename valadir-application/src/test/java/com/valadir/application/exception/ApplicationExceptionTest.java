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
    void constructor_messageAndErrorCode_preservesMessage() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED);

        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
    }

    @Test
    void constructor_messageAndErrorCode_hasNoCause() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED);

        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_messageErrorCodeAndCause_setsErrorCode() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED, CAUSE);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
    }

    @Test
    void constructor_messageErrorCodeAndCause_preservesCause() {

        var exception = new ApplicationException(MESSAGE, ErrorCode.AUTHENTICATION_FAILED, CAUSE);

        assertThat(exception.getCause()).isSameAs(CAUSE);
    }
}
