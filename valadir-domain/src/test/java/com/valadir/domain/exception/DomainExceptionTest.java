package com.valadir.domain.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void constructor_messageAndErrorCode_setsErrorCode() {

        var exception = new DomainException("invariant violated", ErrorCode.REQUIRED_FIELD_MISSING);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
