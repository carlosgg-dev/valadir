package com.valadir.application.otp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PlainOtpTest {

    @Test
    void constructor_validValue_storesValue() {

        var plainOtp = new PlainOtp("123456");

        assertThat(plainOtp.value()).isEqualTo("123456");
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValues_throwsIllegalArgumentException(String blankValue) {

        assertThatIllegalArgumentException().isThrownBy(() -> new PlainOtp(blankValue));
    }

    @Test
    void constructor_lessThanSixDigits_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException().isThrownBy(() -> new PlainOtp("12345"));
    }

    @Test
    void constructor_moreThanSixDigits_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException().isThrownBy(() -> new PlainOtp("1234567"));
    }

    @Test
    void constructor_nonNumeric_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException().isThrownBy(() -> new PlainOtp("abcdef"));
    }

    @Test
    void from_validValue_returnsPlainOtp() {

        var otp = PlainOtp.from("123456");

        assertThat(otp.value()).isEqualTo("123456");
    }

    @Test
    void from_invalidValue_throwsIllegalArgumentException() {

        assertThatIllegalArgumentException().isThrownBy(() -> PlainOtp.from("abc"));
    }

    @Test
    void generate_returnsValueInRange() {

        Stream.generate(PlainOtp::generate)
            .limit(100)
            .map(otp -> Integer.parseInt(otp.value()))
            .forEach(value -> assertThat(value).isBetween(100_000, 999_999));
    }

    @Test
    void generate_calledMultipleTimes_producesUniqueValues() {

        Set<String> codes = Stream.generate(PlainOtp::generate)
            .limit(10)
            .map(PlainOtp::value)
            .collect(Collectors.toSet());

        assertThat(codes).hasSize(10);
    }

    @Test
    void generate_returnsExactlySixDigits() {

        assertThat(PlainOtp.generate().value()).matches("\\d{6}");
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
