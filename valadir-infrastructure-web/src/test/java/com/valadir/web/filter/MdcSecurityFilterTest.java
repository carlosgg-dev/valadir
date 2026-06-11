package com.valadir.web.filter;

import com.valadir.common.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MdcSecurityFilterTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private final MdcSecurityFilter filter = new MdcSecurityFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final Map<String, String> mdcDuringChain = new HashMap<>();
    private final FilterChain capturingChain = (req, res) ->
        Optional.ofNullable(MDC.getCopyOfContextMap()).ifPresent(mdcDuringChain::putAll);

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_jwtAuthentication_exposesAccountIdInMdcDuringChain() throws Exception {

        var jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(ACCOUNT_ID)
            .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain).containsEntry(MdcKeys.ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    void doFilter_noAuthentication_leavesAccountIdUntouched() throws Exception {

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain).doesNotContainKey(MdcKeys.ACCOUNT_ID);
    }

    @Test
    void doFilter_nonJwtAuthentication_leavesAccountIdUntouched() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "credentials"));

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain).doesNotContainKey(MdcKeys.ACCOUNT_ID);
    }
}
