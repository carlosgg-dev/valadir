package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashedPasswordTest {

    @Test
    void shouldCreateHashedPassword_WhenValueIsValid() {

        HashedPassword hashed = new HashedPassword("$2a$10$someHashValue");
        assertThat(hashed.value()).isEqualTo("$2a$10$someHashValue");
    }

    @ParameterizedTest
    @MethodSource("provideBlankHashedPasswords")
    void shouldThrowException_WhenHashedPasswordIsBlank(String blankHash) {

        assertThatThrownBy(() -> new HashedPassword(blankHash))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
    }

    private static String[] provideBlankHashedPasswords() {

        return new String[]{null, "", "   "};
    }
}
