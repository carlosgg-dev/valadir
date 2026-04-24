package com.valadir.application.service;

import java.security.SecureRandom;

final class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_MIN_VALUE = 100_000;
    private static final int OTP_RANGE = 900_000;

    private OtpGenerator() {

    }

    static String generate() {

        return String.valueOf(OTP_MIN_VALUE + SECURE_RANDOM.nextInt(OTP_RANGE));
    }
}
