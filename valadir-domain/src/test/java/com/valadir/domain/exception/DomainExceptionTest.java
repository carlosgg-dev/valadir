package com.valadir.domain.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    private static final String MESSAGE = "invariant violated";

    @Test
    void constructor_messageAndErrorCode_setsErrorCode() {

        var exception = new DomainException(MESSAGE, ErrorCode.REQUIRED_FIELD_MISSING);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void constructor_messageAndErrorCode_preservesMessage() {

        var exception = new DomainException(MESSAGE, ErrorCode.REQUIRED_FIELD_MISSING);

        assertThat(exception.getMessage()).isEqualTo(MESSAGE);
    }
}
