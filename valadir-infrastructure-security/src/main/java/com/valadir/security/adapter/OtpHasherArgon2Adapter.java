package com.valadir.security.adapter;

import com.valadir.application.port.out.OtpHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class OtpHasherArgon2Adapter implements OtpHasher {

    private final Argon2PasswordEncoder encoder;

    public OtpHasherArgon2Adapter(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
    }

    @Override
    public String hash(String plainCode) {

        return encoder.encode(plainCode);
    }

    @Override
    public boolean matches(String plainCode, String hashedCode) {

        return encoder.matches(plainCode, hashedCode);
    }
}
