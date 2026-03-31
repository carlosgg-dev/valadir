package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountIdTest {

    @Test
    void shouldCreateAccountId_WhenValueIsValid() {

        AccountId id = AccountId.generate();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void shouldGenerateUniqueIds() {

        AccountId first = AccountId.generate();
        AccountId second = AccountId.generate();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldReconstitute_WhenFromExistingUUID() {

        UUID uuid = UUID.randomUUID();
        AccountId id = AccountId.from(uuid);
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowException_WhenFromNullUUID() {

        assertThatThrownBy(() -> AccountId.from(null))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
