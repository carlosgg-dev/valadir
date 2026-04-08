package com.valadir.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.web.exception.JwtAccessDeniedHandler;
import com.valadir.web.exception.JwtAuthenticationEntryPoint;
import com.valadir.web.filter.MdcRequestFilter;
import com.valadir.web.filter.MdcSecurityFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        final HttpSecurity http,
        final JwtDecoder jwtDecoder,
        final ObjectMapper objectMapper
    ) throws Exception {

        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new MdcRequestFilter(), SecurityContextHolderFilter.class)
            .addFilterAfter(new MdcSecurityFilter(), BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                                 ApiRoutes.Auth.REGISTER_PATH,
                                 ApiRoutes.Auth.LOGIN_PATH,
                                 ApiRoutes.Auth.REFRESH_PATH
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(new JwtAccessDeniedHandler(objectMapper))
            )
            .build();
    }
}
