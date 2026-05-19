package com.valadir.application.port.out;

public interface OtpHasher {

    String hash(String plainCode);

    boolean matches(String plainCode, String hashedCode);

    // Runs the same hashing work as matches() but discards the result.
    // Call when no OTP exists for a given account to prevent timing-based account enumeration.
    void guardTiming();
}
