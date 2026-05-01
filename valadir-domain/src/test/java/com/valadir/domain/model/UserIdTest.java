package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserIdTest {

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
    void from_existingUuid_createsUserId() {

        UUID uuid = UUID.randomUUID();
        UserId id = UserId.from(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void from_nullUuid_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> UserId.from(null))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
