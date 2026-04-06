package com.valadir.domain.service;

import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;

public interface PasswordHasher {

    HashedPassword hash(RawPassword password);

    boolean matches(RawPassword rawPassword, HashedPassword hashedPassword);

    // Runs the same hashing work as matches() but discards the result.
    // Call this when the target hash does not exist to prevent timing-based account enumeration.
    void guardTiming(RawPassword rawPassword);
}
