package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public record PlainOtp(String value) {

    private static final Pattern PATTERN = Pattern.compile("\\d{6}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_MIN_VALUE = 100_000;
    private static final int OTP_RANGE = 900_000;

    public PlainOtp {

        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new DomainException("PlainOtp must be exactly 6 numeric digits", ErrorCode.INVALID_OTP);
        }
    }

    public static PlainOtp from(String value) {

        return new PlainOtp(value);
    }

    public static PlainOtp generate() {

        return new PlainOtp(String.valueOf(OTP_MIN_VALUE + SECURE_RANDOM.nextInt(OTP_RANGE)));
    }
}
