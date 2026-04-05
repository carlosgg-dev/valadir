package com.valadir.security.config;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.text.ParseException;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class JwtConfig {

    @Bean
    JwtEncoder jwtEncoder(final JwtProperties properties) throws ParseException {

        final ECKey ecKey = ECKey.parse(properties.privateKey());
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(ecKey)));
    }
}
