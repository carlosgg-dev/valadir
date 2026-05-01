package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AccountIdTest {

    @Test
    void generate_createsNonNullId() {

        AccountId id = AccountId.generate();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void generate_consecutiveCalls_returnsUniqueIds() {

        AccountId first = AccountId.generate();
        AccountId second = AccountId.generate();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void from_existingUuid_reconstitutesId() {

        UUID uuid = UUID.randomUUID();
        AccountId id = AccountId.from(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void from_nullUuid_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> AccountId.from(null))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
