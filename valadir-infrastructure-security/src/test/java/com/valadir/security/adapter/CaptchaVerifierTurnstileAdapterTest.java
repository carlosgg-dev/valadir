package com.valadir.security.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaptchaVerifierTurnstileAdapterTest {

    private static final String VERIFY_URL = "https://example.com/test";
    private static final String SECRET = "test-secret";
    private static final String TOKEN = "test-token";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {

        restClientBuilder = RestClient.builder().messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter()));
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void isValid_disabled_returnsTrueWithoutCallingProvider() {

        var adapter = new CaptchaVerifierTurnstileAdapter(restClientBuilder.build(), VERIFY_URL, SECRET, false, closedCircuitBreaker());

        assertThat(adapter.isValid(null)).isTrue();
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("blankTokens")
    void isValid_blankToken_returnsFalseWithoutCallingProvider(String blankToken) {

        assertThat(enabledAdapter().isValid(blankToken)).isFalse();
        server.verify();
    }

    @Test
    void isValid_successResponse_returnsTrue() {

        server.expect(requestTo(VERIFY_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().formDataContains(Map.of("secret", SECRET, "response", TOKEN)))
            .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThat(enabledAdapter().isValid(TOKEN)).isTrue();
        server.verify();
    }

    @Test
    void isValid_emptyResponseBody_returnsFalse() {

        server.expect(requestTo(VERIFY_URL))
            .andRespond(withSuccess());

        assertThat(enabledAdapter().isValid(TOKEN)).isFalse();
        server.verify();
    }

    @Test
    void isValid_failureResponse_returnsFalse() {

        server.expect(requestTo(VERIFY_URL))
            .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThat(enabledAdapter().isValid(TOKEN)).isFalse();
        server.verify();
    }

    @Test
    void isValid_serverError_failsOpenReturnsTrue() {

        server.expect(requestTo(VERIFY_URL))
            .andRespond(withServerError());

        assertThat(enabledAdapter().isValid(TOKEN)).isTrue();
        server.verify();
    }

    @Test
    void isValid_openCircuit_failsOpenWithoutCallingProvider() {

        // No expectation is registered, so the request never leaving is what keeps this green:
        // the outage is answered without paying the connect + read timeout again.
        assertThat(enabledAdapter(openCircuitBreaker()).isValid(TOKEN)).isTrue();
        server.verify();
    }

    @Test
    void isValid_unparseableResponse_failsClosedReturnsFalse() {

        server.expect(requestTo(VERIFY_URL))
            .andRespond(withSuccess("<html>not json</html>", MediaType.TEXT_HTML));

        assertThat(enabledAdapter().isValid(TOKEN)).isFalse();
        server.verify();
    }

    @Test
    void isValid_unknownFieldsWithStrictMapper_toleratesThemReturnsTrue() {

        // Guards @JsonIgnoreProperties: the adapter must tolerate Turnstile's extra fields.
        // Removing the annotation makes parsing fail -> fail-closed -> false, turning this assertion red.
        var responseWithExtraFields = """
            {"success":true,"challenge_ts":"2026-06-24T00:00:00Z","hostname":"example.com","error-codes":[]}""";

        // Default ObjectMapper keeps FAIL_ON_UNKNOWN_PROPERTIES enabled, so only the record's
        // @JsonIgnoreProperties can keep parsing alive against the extra fields.
        var defaultStrictMapper = new ObjectMapper();

        RestClient.Builder strictBuilder = RestClient.builder()
            .messageConverters(converters -> {
                converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                converters.add(new MappingJackson2HttpMessageConverter(defaultStrictMapper));
            });

        MockRestServiceServer strictServer = MockRestServiceServer.bindTo(strictBuilder).build();
        strictServer.expect(requestTo(VERIFY_URL))
            .andRespond(withSuccess(responseWithExtraFields, MediaType.APPLICATION_JSON));

        var adapter = new CaptchaVerifierTurnstileAdapter(strictBuilder.build(), VERIFY_URL, SECRET, true, closedCircuitBreaker());

        assertThat(adapter.isValid(TOKEN)).isTrue();
        strictServer.verify();
    }

    private CaptchaVerifierTurnstileAdapter enabledAdapter() {

        return enabledAdapter(closedCircuitBreaker());
    }

    private CaptchaVerifierTurnstileAdapter enabledAdapter(CircuitBreaker circuitBreaker) {

        return new CaptchaVerifierTurnstileAdapter(restClientBuilder.build(), VERIFY_URL, SECRET, true, circuitBreaker);
    }

    /**
     * A fresh, closed circuit per call, so no state leaks between tests. The new registry is what
     * makes it fresh: a shared one caches by name and would hand back the same instance, carrying
     * its call count and state over. The default config needs 100 calls before it can open, well
     * beyond what any test makes.
     */
    private static CircuitBreaker closedCircuitBreaker() {

        return CircuitBreakerRegistry.ofDefaults().circuitBreaker("test");
    }

    private static CircuitBreaker openCircuitBreaker() {

        var circuitBreaker = closedCircuitBreaker();
        circuitBreaker.transitionToOpenState();
        return circuitBreaker;
    }

    private static String[] blankTokens() {

        return new String[]{null, "", " "};
    }
}
