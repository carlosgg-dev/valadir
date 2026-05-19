package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.UUID;

public class OtpHasherArgon2Adapter implements OtpHasher {

    private final Argon2PasswordEncoder encoder;
    // Computed once at startup with a random value to equalize response time in guardTiming()
    private final String dummyPlainCode;
    private final String dummyHashedCode;

    public OtpHasherArgon2Adapter(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
        this.dummyPlainCode = UUID.randomUUID().toString();
        this.dummyHashedCode = encoder.encode(dummyPlainCode);
    }

    @Override
    public String hash(String plainCode) {

        return encoder.encode(plainCode);
    }

    @Override
    public boolean matches(String plainCode, String hashedCode) {

        return encoder.matches(plainCode, hashedCode);
    }

    @Override
    public void guardTiming() {

        encoder.matches(dummyPlainCode, dummyHashedCode);
    }
}
