package com.valadir.security.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    // OWASP's Argon2id recommendation. Every hash carries its parameters, so the encoded prefix is
    // both what the verifier reads back and the only place a downgrade would show.
    private static final String OWASP_PARAMETERS = "$argon2id$v=19$m=19456,t=2,p=1$";

    @Test
    void argon2PasswordEncoder_anyValue_encodesWithTheOwaspParameters() {

        var encoded = new PasswordEncoderConfig().argon2PasswordEncoder().encode("any-value");

        assertThat(encoded).startsWith(OWASP_PARAMETERS);
    }
}
