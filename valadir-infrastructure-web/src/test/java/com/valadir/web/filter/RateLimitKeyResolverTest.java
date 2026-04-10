package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Strategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    private static final String CLIENT_IP = "10.0.0.1";
    private static final String ACCOUNT_ID = "account-uuid-123";
    private static final String EMAIL = "user@example.com";
    private static final String PATH = "/api/auth/login/";
    private static final String NORMALIZED_PATH = "api_auth_login";
    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW = 60;

    private RateLimitKeyResolver resolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        resolver = new RateLimitKeyResolver(objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_ipStrategy_returnsKeyContainingClientIpAndPath() {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.IP, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:ip:" + NORMALIZED_PATH + ":" + CLIENT_IP);
    }

    @Test
    void resolve_ipStrategy_withHeaderXForwardedForUsesFirstIpInChain() {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.IP, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 192.168.1.1");

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:ip:" + NORMALIZED_PATH + ":203.0.113.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void resolve_ipStrategy_withBlankHeaderXForwardedForUsesClientIp(String header) {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.IP, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.addHeader("X-Forwarded-For", header);

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:ip:" + NORMALIZED_PATH + ":" + CLIENT_IP);
    }

    @Test
    void resolve_emailStrategy_withEmailReturnsKeyContainingEmailAndPath() throws Exception {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.EMAIL, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", EMAIL, "password", "secret")));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:email:" + NORMALIZED_PATH + ":" + EMAIL);
    }

    @Test
    void resolve_emailStrategy_missingEmailInBodyReturnsEmpty() throws Exception {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.EMAIL, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.setContent(objectMapper.writeValueAsBytes(Map.of("password", "secret")));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void resolve_emailStrategy_blankEmailReturnsEmpty(String email) throws Exception {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.EMAIL, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", email, "password", "secret")));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).isEmpty();
    }

    @Test
    void resolve_emailStrategy_invalidJsonReturnsEmpty() {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.EMAIL, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();
        request.setContent("not-json".getBytes());
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).isEmpty();
    }

    @Test
    void resolve_userStrategy_authenticatedReturnsKeyContainingAccountId() {

        authenticate();
        final var rule = new RateLimitProperties.Rule(PATH, Strategy.USER, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:user:" + ACCOUNT_ID);
    }

    @Test
    void resolve_userStrategy_unauthenticatedReturnsEmpty() {

        final var rule = new RateLimitProperties.Rule(PATH, Strategy.USER, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).isEmpty();
    }

    @Test
    void resolve_userStrategy_nonJwtAuthenticationReturnsEmpty() {

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ACCOUNT_ID, null));
        final var rule = new RateLimitProperties.Rule(PATH, Strategy.USER, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).isEmpty();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("pathNormalizationCases")
    void resolve_ipStrategy_normalizesPathCorrectly(final String path, final String expectedNormalized) {

        final var rule = new RateLimitProperties.Rule(path, Strategy.IP, MAX_REQUESTS, WINDOW);
        final MockHttpServletRequest request = buildRequest();

        final Optional<String> key = resolver.resolve(request, rule);

        assertThat(key).hasValue("rate_limit:ip:" + expectedNormalized + ":" + CLIENT_IP);
    }

    static Stream<Arguments> pathNormalizationCases() {

        return Stream.of(
            Arguments.of("/api/auth/login", "api_auth_login"),
            Arguments.of("api/auth/login/", "api_auth_login"),
            Arguments.of("/api/auth/login/", "api_auth_login"),
            Arguments.of("api/auth/login", "api_auth_login"),
            Arguments.of("/API/AUTH/LOGIN", "api_auth_login"),
            Arguments.of("/api/v2/users/profile", "api_v2_users_profile"),
            Arguments.of("/api//double-slash", "api_double_slash")
        );
    }

    private MockHttpServletRequest buildRequest() {

        final var request = new MockHttpServletRequest();
        request.setRequestURI(PATH);
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }

    private void authenticate() {

        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "ES256")
            .subject(ACCOUNT_ID)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
