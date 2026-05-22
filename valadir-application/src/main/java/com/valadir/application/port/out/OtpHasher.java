package com.valadir.application.port.out;

import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;

public interface OtpHasher {

    HashedOtp hash(PlainOtp plainOtp);

    boolean matches(PlainOtp plainOtp, HashedOtp hashedOtp);

    // Runs the same hashing work as matches() but discards the result.
    // Call when no OTP exists for a given account to prevent timing-based account enumeration.
    void guardTiming();
}
