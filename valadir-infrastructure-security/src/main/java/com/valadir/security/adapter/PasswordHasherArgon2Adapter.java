package com.valadir.security.adapter;

import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.UUID;

public class PasswordHasherArgon2Adapter implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;
    // Computed once at startup with a random value to equalize response time in decoyMatch()
    private final String decoyHash;

    public PasswordHasherArgon2Adapter(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
        this.decoyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public HashedPassword hash(RawPassword password) {

        return new HashedPassword(encoder.encode(password.value()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, HashedPassword hashedPassword) {

        return encoder.matches(rawPassword.value(), hashedPassword.value());
    }

    @Override
    public void decoyMatch(RawPassword rawPassword) {

        encoder.matches(rawPassword.value(), decoyHash);
    }
}
