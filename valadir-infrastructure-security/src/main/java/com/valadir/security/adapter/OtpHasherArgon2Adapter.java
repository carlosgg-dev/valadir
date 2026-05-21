package com.valadir.security.adapter;

import com.valadir.application.otp.HashedOtp;
import com.valadir.application.otp.PlainOtp;
import com.valadir.application.port.out.OtpHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class OtpHasherArgon2Adapter implements OtpHasher {

    private final Argon2PasswordEncoder encoder;
    // Computed once at startup with a random value to equalize response time in guardTiming()
    private final PlainOtp dummyPlainOtp;
    private final HashedOtp dummyHashedOtp;

    public OtpHasherArgon2Adapter(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
        this.dummyPlainOtp = PlainOtp.generate();
        this.dummyHashedOtp = new HashedOtp(encoder.encode(dummyPlainOtp.value()));
    }

    @Override
    public HashedOtp hash(PlainOtp plainOtp) {

        return new HashedOtp(encoder.encode(plainOtp.value()));
    }

    @Override
    public boolean matches(PlainOtp plainOtp, HashedOtp hashedOtp) {

        return encoder.matches(plainOtp.value(), hashedOtp.value());
    }

    @Override
    public void guardTiming() {

        encoder.matches(dummyPlainOtp.value(), dummyHashedOtp.value());
    }
}
