package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

public record PlainOtp(String value) {

    private static final Pattern PATTERN = Pattern.compile("\\d{6}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_EXCLUSIVE_BOUND = 1_000_000;

    public PlainOtp {

        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new DomainException("PlainOtp must be exactly 6 numeric digits", ErrorCode.INVALID_OTP);
        }
    }

    public static PlainOtp from(String value) {

        return new PlainOtp(value);
    }

    // Padded, never offset past the leading zeros: a generator narrower than the pattern above would
    // withhold a tenth of the codes it declares. Locale.ROOT so the digits cannot come out in another script
    public static PlainOtp generate() {

        return new PlainOtp(String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(OTP_EXCLUSIVE_BOUND)));
    }
}
