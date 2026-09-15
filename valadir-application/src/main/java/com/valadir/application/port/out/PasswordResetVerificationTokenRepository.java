package com.valadir.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface PasswordResetVerificationTokenRepository {

    void save(String verificationToken, PasswordResetVerification verification, Duration ttl);

    Optional<PasswordResetVerification> verificationFor(String verificationToken);

    void delete(String verificationToken);
}
