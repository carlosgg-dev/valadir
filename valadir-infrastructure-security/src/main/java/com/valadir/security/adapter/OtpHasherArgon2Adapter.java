package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpHasher;
import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.UUID;

public class OtpHasherArgon2Adapter implements OtpHasher {

    private final Argon2PasswordEncoder encoder;
    // Computed once at startup with a random value to equalize response time in decoyMatch()
    private final String decoyOtp;
    private final String decoyHashedOtp;

    public OtpHasherArgon2Adapter(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
        this.decoyOtp = UUID.randomUUID().toString();
        this.decoyHashedOtp = encoder.encode(decoyOtp);
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
    public void decoyMatch() {

        encoder.matches(decoyOtp, decoyHashedOtp);
    }
}
