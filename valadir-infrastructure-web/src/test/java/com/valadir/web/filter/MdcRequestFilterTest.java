package com.valadir.web.filter;

import com.valadir.common.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MdcRequestFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String EXTERNAL_REQUEST_ID = "external-request-id";

    private final MdcRequestFilter filter = new MdcRequestFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final Map<String, String> mdcDuringChain = new HashMap<>();
    private final FilterChain capturingChain = (req, res) -> mdcDuringChain.putAll(MDC.getCopyOfContextMap());

    @AfterEach
    void tearDown() {

        MDC.clear();
    }

    @Test
    void doFilter_headerPresent_propagatesRequestIdToMdcAndResponse() throws Exception {

        request.addHeader(REQUEST_ID_HEADER, EXTERNAL_REQUEST_ID);

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain).containsEntry(MdcKeys.REQUEST_ID, EXTERNAL_REQUEST_ID);
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(EXTERNAL_REQUEST_ID);
    }

    @Test
    void doFilter_missingHeader_generatesUuidRequestId() throws Exception {

        filter.doFilter(request, response, capturingChain);

        var generatedId = mdcDuringChain.get(MdcKeys.REQUEST_ID);
        assertThat(UUID.fromString(generatedId)).hasToString(generatedId);
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(generatedId);
    }

    @Test
    void doFilter_blankHeader_generatesUuidRequestIdInstead() throws Exception {

        request.addHeader(REQUEST_ID_HEADER, " ");

        filter.doFilter(request, response, capturingChain);

        var generatedId = mdcDuringChain.get(MdcKeys.REQUEST_ID);
        assertThat(UUID.fromString(generatedId)).hasToString(generatedId);
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(generatedId);
    }

    @Test
    void doFilter_anyRequest_exposesMethodPathAndUnknownAccountDuringChain() throws Exception {

        filter.doFilter(request, response, capturingChain);

        assertThat(mdcDuringChain)
            .containsEntry(MdcKeys.METHOD, "POST")
            .containsEntry(MdcKeys.PATH, "/api/auth/login")
            .containsEntry(MdcKeys.ACCOUNT_ID, MdcKeys.UNKNOWN);
    }

    @Test
    void doFilter_chainCompletes_clearsMdc() throws Exception {

        filter.doFilter(request, response, capturingChain);

        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
    }

    @Test
    void doFilter_chainThrows_stillClearsMdc() {

        FilterChain failingChain = (req, res) -> {
            throw new ServletException("downstream failure");
        };

        assertThatExceptionOfType(ServletException.class)
            .isThrownBy(() -> filter.doFilter(request, response, failingChain));

        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
    }
}
