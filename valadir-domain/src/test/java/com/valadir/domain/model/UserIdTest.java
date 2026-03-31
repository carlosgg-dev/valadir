package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void shouldCreateUserId_WhenValueIsValid() {

        UserId id = UserId.generate();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void shouldGenerateUniqueIds() {

        UserId first = UserId.generate();
        UserId second = UserId.generate();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldCreateUserId_WhenFromExistingUUID() {

        UUID uuid = UUID.randomUUID();
        UserId id = UserId.from(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowException_WhenFromNullUUID() {

        assertThatThrownBy(() -> UserId.from(null))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
