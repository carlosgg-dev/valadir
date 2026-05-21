package com.valadir.application.otp;

public record HashedOtp(String value) {

    public HashedOtp {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HashedOtp value must not be blank");
        }
    }
}
