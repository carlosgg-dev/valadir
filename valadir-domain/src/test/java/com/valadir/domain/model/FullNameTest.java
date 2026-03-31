package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FullNameTest {

    @Test
    void shouldCreateFullName_WhenValueIsValid() {

        FullName fullName = new FullName("Bruce Wayne");
        assertThat(fullName.value()).isEqualTo("Bruce Wayne");
    }

    @ParameterizedTest
    @MethodSource("provideBlankFullNames")
    void shouldThrowException_WhenValueIsBlank(String blankFullName) {

        assertThatThrownBy(() -> new FullName(blankFullName))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void shouldCreateFullName_WhenValueIsExactlyMinLength() {

        FullName fullName = new FullName("Wa");
        assertThat(fullName.value()).isEqualTo("Wa");
    }

    @Test
    void shouldThrowException_WhenValueItsShort() {

        assertThatThrownBy(() -> new FullName("W"))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] provideBlankFullNames() {

        return new String[]{null, "", "   "};
    }
}
