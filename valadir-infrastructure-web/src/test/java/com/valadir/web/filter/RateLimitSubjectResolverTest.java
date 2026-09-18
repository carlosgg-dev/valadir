package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.ratelimit.RateLimitStrategy;
import com.valadir.common.ratelimit.RateLimitSubject;
import com.valadir.web.config.RateLimitProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitSubjectResolverTest {

    private static final String CLIENT_IP = "10.0.0.1";
    private static final String ACCOUNT_ID = "account-uuid-123";
    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String PATH = "/api/auth/login/";
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private RateLimitSubjectResolver resolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        resolver = new RateLimitSubjectResolver(objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_ipStrategy_returnsSubjectCarryingTheClientIpAndTheRulePath() {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.IP, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.IP, PATH, CLIENT_IP));
    }

    // Nothing but the address the container reports reaches the subject. Reading the header would
    // hand the caller its own bucket, and one fresh bucket for every value it cares to invent.
    @Test
    void resolve_ipStrategy_spoofedXForwardedFor_stillKeysOnTheClientIp() {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.IP, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 192.168.1.1");

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.IP, PATH, CLIENT_IP));
    }

    @Test
    void resolve_emailStrategy_withEmailReturnsSubjectCarryingTheAddress() throws Exception {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequestWithBody(Map.of("email", EMAIL, "password", "secret"));

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.EMAIL, PATH, EMAIL));
    }

    // Same subject for the same address however it is typed: otherwise changing the case would hand
    // out a fresh counter and every per-email limit could be walked around.
    @ParameterizedTest
    @ValueSource(strings = {
        "Bruce.Wayne@Email.com",
        "BRUCE.WAYNE@EMAIL.COM",
        "  bruce.wayne@email.com  ",
        " Bruce.Wayne@Email.com "
    })
    void resolve_emailStrategy_normalizesTheEmailIntoASingleSubject(String email) throws Exception {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequestWithBody(Map.of("email", email, "password", "secret"));

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.EMAIL, PATH, EMAIL));
    }

    // U+00F1 is the ñ as one character, n + U+0303 is an n carrying a combining tilde: one address on
    // screen, two to a bucket keyed by bytes. The account resolves to the composed form, so a subject
    // built from the decomposed one would count the same address twice. Written as escapes so a tool
    // that normalized this file cannot turn the two fixtures into one.
    @Test
    void resolve_emailStrategy_theSameLetterSpelledTwoWays_resolvesToTheStoredForm() throws Exception {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequestWithBody(Map.of("email", "pen\u0303a@espan\u0303a.com", "password", "secret"));

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.EMAIL, PATH, "pe\u00F1a@espa\u00F1a.com"));
    }

    @Test
    void resolve_emailStrategy_missingEmailInBodyReturnsEmpty() throws Exception {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequestWithBody(Map.of("password", "secret"));

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void resolve_emailStrategy_blankEmailReturnsEmpty(String email) throws Exception {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequestWithBody(Map.of("email", email, "password", "secret"));

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).isEmpty();
    }

    @Test
    void resolve_emailStrategy_invalidJsonReturnsEmpty() {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.EMAIL, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();
        request.setContent("not-json".getBytes());
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).isEmpty();
    }

    @Test
    void resolve_userStrategy_authenticatedReturnsSubjectCarryingTheAccountId() {

        authenticate();
        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.USER, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).hasValue(new RateLimitSubject(RateLimitStrategy.USER, PATH, ACCOUNT_ID));
    }

    @Test
    void resolve_userStrategy_unauthenticatedReturnsEmpty() {

        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.USER, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).isEmpty();
    }

    @Test
    void resolve_userStrategy_nonJwtAuthenticationReturnsEmpty() {

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ACCOUNT_ID, null));
        var rule = new RateLimitProperties.Rule(PATH, HttpMethod.POST, RateLimitStrategy.USER, MAX_REQUESTS, WINDOW);
        MockHttpServletRequest request = buildRequest();

        Optional<RateLimitSubject> subject = resolver.resolve(request, rule);

        assertThat(subject).isEmpty();
    }

    private MockHttpServletRequest buildRequest() {

        var request = new MockHttpServletRequest();
        request.setRequestURI(PATH);
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }

    private MockHttpServletRequest buildRequestWithBody(Map<String, String> body) throws Exception {

        MockHttpServletRequest request = buildRequest();
        request.setContent(objectMapper.writeValueAsBytes(body));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    private void authenticate() {

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "ES256")
            .subject(ACCOUNT_ID)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
