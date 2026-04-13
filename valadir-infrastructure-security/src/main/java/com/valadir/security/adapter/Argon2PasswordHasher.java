package com.valadir.security.adapter;

import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;
    // Computed once at startup with a random value to equalize response time in guardTiming().
    private final String dummyHash;

    Argon2PasswordHasher(Argon2PasswordEncoder encoder) {

        this.encoder = encoder;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
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
    public void guardTiming(RawPassword rawPassword) {

        encoder.matches(rawPassword.value(), dummyHash);
    }
}
