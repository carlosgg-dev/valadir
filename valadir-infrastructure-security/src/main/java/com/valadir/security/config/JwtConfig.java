package com.valadir.security.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.text.ParseException;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class JwtConfig {

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) throws ParseException {

        ECKey ecKey = ECKey.parse(properties.privateKey());
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(ecKey)));
    }

    @Bean("nimbusJwtDecoder")
    JwtDecoder nimbusJwtDecoder(JwtProperties properties) throws ParseException {

        ECKey publicKey = ECKey.parse(properties.privateKey()).toPublicJWK();
        var jwkSource = new ImmutableJWKSet<>(new JWKSet(publicKey));
        var processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource));
        return new NimbusJwtDecoder(processor);
    }
}
