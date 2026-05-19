package com.valadir.common.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfrastructureExceptionTest {

    private static final RuntimeException CAUSE = new RuntimeException("root cause");

    @Test
    void constructor_messageOnly_setsInfrastructureUnavailableCode() {

        var exception = new InfrastructureException("something failed");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void constructor_messageOnly_preservesMessage() {

        var exception = new InfrastructureException("something failed");

        assertThat(exception.getMessage()).isEqualTo("something failed");
    }

    @Test
    void constructor_messageOnly_hasNoCause() {

        var exception = new InfrastructureException("something failed");

        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_messageAndCause_setsInfrastructureUnavailableCode() {

        var exception = new InfrastructureException("something failed", CAUSE);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void constructor_messageAndCause_preservesCause() {

        var exception = new InfrastructureException("something failed", CAUSE);

        assertThat(exception.getCause()).isSameAs(CAUSE);
    }
}
