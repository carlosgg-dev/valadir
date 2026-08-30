package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PlainOtpTest {

    @Test
    void constructor_validValue_storesValue() {

        var plainOtp = new PlainOtp("123456");
        assertThat(plainOtp.value()).isEqualTo("123456");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new PlainOtp(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_OTP);
    }

    @Test
    void constructor_lessThanSixDigits_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new PlainOtp("12345"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_OTP);
    }

    @Test
    void constructor_moreThanSixDigits_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new PlainOtp("1234567"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_OTP);
    }

    @Test
    void constructor_nonNumeric_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new PlainOtp("abcdef"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_OTP);
    }

    @Test
    void from_validValue_returnsPlainOtp() {

        var otp = PlainOtp.from("123456");
        assertThat(otp).isEqualTo(new PlainOtp("123456"));
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

}
