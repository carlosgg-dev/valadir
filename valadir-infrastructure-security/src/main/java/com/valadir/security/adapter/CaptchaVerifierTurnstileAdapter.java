package com.valadir.security.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.valadir.application.port.out.CaptchaVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class CaptchaVerifierTurnstileAdapter implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(CaptchaVerifierTurnstileAdapter.class);

    private final RestClient restClient;
    private final String verifyUrl;
    private final String secret;
    private final boolean enabled;

    public CaptchaVerifierTurnstileAdapter(RestClient restClient, String verifyUrl, String secret, boolean enabled) {

        this.restClient = restClient;
        this.verifyUrl = verifyUrl;
        this.secret = secret;
        this.enabled = enabled;
    }

    @Override
    public boolean isValid(String token) {

        if (!enabled) {
            return true;
        }

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            return verifyToken(token);

        } catch (ResourceAccessException | HttpServerErrorException e) {
            // Provider down (I/O, timeout, 5xx): fail-open to avoid blocking all logins on an outage.
            log.warn("Turnstile unavailable — allowing challenged login attempt", e);
            return true;

        } catch (RestClientException e) {
            // Response received but uninterpretable (4xx, bad body): not an outage, so fail-closed
            // rather than silently disable the CAPTCHA.
            log.error("Turnstile returned an uninterpretable response — denying challenged login attempt", e);
            return false;
        }
    }

    private boolean verifyToken(String token) {

        TurnstileResponse response = restClient.post()
            .uri(verifyUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formBody(token))
            .retrieve()
            .body(TurnstileResponse.class);

        return response != null && response.success();
    }

    private MultiValueMap<String, String> formBody(String token) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secret);
        body.add("response", token);
        return body;
    }

    // Only 'success' is mapped from response, ignoreUnknown tolerates Turnstile's extra fields regardless of the global Jackson config.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TurnstileResponse(boolean success) {

    }
}
