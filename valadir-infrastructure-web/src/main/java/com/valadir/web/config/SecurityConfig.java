package com.valadir.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.exception.HttpStatusResolver;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    private static final String[] POST_PUBLIC_ROUTES = {
        ApiRoutes.Auth.Registration.REGISTER_PATH,
        ApiRoutes.Auth.Registration.ACTIVATE_PATH,
        ApiRoutes.Auth.Registration.RESEND_PATH,
        ApiRoutes.Auth.Session.LOGIN_PATH,
        ApiRoutes.Auth.Session.REFRESH_PATH,
        ApiRoutes.Auth.PasswordReset.INITIATE_PATH,
        ApiRoutes.Auth.PasswordReset.VERIFY_PATH,
        ApiRoutes.Auth.PasswordReset.COMPLETE_PATH
    };

    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public SecurityConfig(ObjectMapper objectMapper, RateLimiter rateLimiter, RateLimitProperties rateLimitProperties) {

        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Bean
    HttpStatusResolver httpStatusResolver() {

        return new HttpStatusResolver();
    }

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter() {

        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(SecurityErrorResponseWriter securityErrorResponseWriter) {

        return new JwtAuthenticationEntryPoint(securityErrorResponseWriter);
    }

    @Bean
    JwtAccessDeniedHandler jwtAccessDeniedHandler(SecurityErrorResponseWriter securityErrorResponseWriter) {

        return new JwtAccessDeniedHandler(securityErrorResponseWriter);
    }

    @Bean
    RateLimitKeyResolver rateLimitKeyResolver() {

        return new RateLimitKeyResolver(objectMapper);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        var authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return authConverter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtDecoder jwtDecoder,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        JwtAccessDeniedHandler jwtAccessDeniedHandler,
        RateLimitKeyResolver rateLimitKeyResolver
    ) throws Exception {

        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new MdcRequestFilter(), SecurityContextHolderFilter.class)
            .addFilterAfter(new RateLimitFilter(rateLimiter, rateLimitProperties, new RateLimitResponseWriter(objectMapper), rateLimitKeyResolver),
                            BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new MdcSecurityFilter(), RateLimitFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, POST_PUBLIC_ROUTES).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .build();
    }
}
