package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserIdTest {

    @Test
    void constructor_validValue_createsUserId() {

        UUID uuid = UUID.randomUUID();
        UserId id = new UserId(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void constructor_nullValue_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new UserId(null))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void generate_createsNonNullId() {

        UserId id = UserId.generate();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void generate_consecutiveCalls_returnsUniqueIds() {

        UserId first = UserId.generate();
        UserId second = UserId.generate();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void from_validValue_createsUserId() {

        UUID uuid = UUID.randomUUID();
        UserId id = UserId.from(uuid);
        assertThat(id).isEqualTo(new UserId(uuid));
    }
}
