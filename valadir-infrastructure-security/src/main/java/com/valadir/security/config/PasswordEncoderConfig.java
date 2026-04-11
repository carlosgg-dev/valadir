package com.valadir.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration
class PasswordEncoderConfig {

    @Bean
    Argon2PasswordEncoder argon2PasswordEncoder() {

        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
