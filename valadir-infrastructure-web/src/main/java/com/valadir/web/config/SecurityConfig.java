package com.valadir.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.exception.JwtAccessDeniedHandler;
import com.valadir.web.exception.JwtAuthenticationEntryPoint;
import com.valadir.web.exception.SecurityErrorResponseWriter;
import com.valadir.web.filter.MdcRequestFilter;
import com.valadir.web.filter.MdcSecurityFilter;
import com.valadir.web.filter.RateLimitFilter;
import com.valadir.web.filter.RateLimitKeyResolver;
import com.valadir.web.filter.RateLimitResponseWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public SecurityConfig(
        final ObjectMapper objectMapper,
        final RateLimiter rateLimiter,
        final RateLimitProperties rateLimitProperties
    ) {

        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter() {

        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(final SecurityErrorResponseWriter securityErrorResponseWriter) {

        return new JwtAuthenticationEntryPoint(securityErrorResponseWriter);
    }

    @Bean
    JwtAccessDeniedHandler jwtAccessDeniedHandler(final SecurityErrorResponseWriter securityErrorResponseWriter) {

        return new JwtAccessDeniedHandler(securityErrorResponseWriter);
    }

    @Bean
    RateLimitKeyResolver rateLimitKeyResolver() {

        return new RateLimitKeyResolver(objectMapper);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        final HttpSecurity http,
        final JwtDecoder jwtDecoder,
        final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        final JwtAccessDeniedHandler jwtAccessDeniedHandler,
        final RateLimitKeyResolver rateLimitKeyResolver
    ) throws Exception {

        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new MdcRequestFilter(), SecurityContextHolderFilter.class)
            .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimitProperties, new RateLimitResponseWriter(objectMapper), rateLimitKeyResolver),
                            BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new MdcSecurityFilter(), RateLimitFilter.class)
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
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .build();
    }
}
