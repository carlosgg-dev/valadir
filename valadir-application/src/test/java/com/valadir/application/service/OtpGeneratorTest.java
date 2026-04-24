package com.valadir.application.service;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OtpGeneratorTest {

    @Test
    void generate_returnsExactlySixDigits() {

        assertThat(OtpGenerator.generate()).matches("\\d{6}");
    }

    @Test
    void generate_returnsValueInRange() {

        Stream.generate(OtpGenerator::generate)
            .limit(100)
            .map(Integer::parseInt)
            .forEach(value -> assertThat(value).isBetween(100_000, 999_999));
    }

    @Test
    void generate_calledMultipleTimes_producesUniqueValues() {

        Set<String> codes = Stream.generate(OtpGenerator::generate)
            .limit(10)
            .collect(Collectors.toSet());

        assertThat(codes).hasSize(10);
    }
}
