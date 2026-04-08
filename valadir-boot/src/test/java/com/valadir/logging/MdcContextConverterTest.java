package com.valadir.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.valadir.common.mdc.MdcKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MdcContextConverterTest {

    @Mock
    private ILoggingEvent event;

    @InjectMocks
    private MdcContextConverter converter;

    @Test
    void convert_allFieldsPresent_returnsFormattedBlock() {

        given(event.getMDCPropertyMap()).willReturn(Map.of(
            MdcKeys.REQUEST_ID, "abc-123",
            MdcKeys.METHOD, "POST",
            MdcKeys.PATH, "/api/auth/login",
            MdcKeys.ACCOUNT_ID, "123456789"
        ));

        assertThat(converter.convert(event))
            .isEqualTo("[requestId=abc-123 method=POST path=/api/auth/login accountId=123456789] ");
    }

    @Test
    void convert_requestIdAbsent_returnsEmptyString() {

        given(event.getMDCPropertyMap()).willReturn(Map.of());

        assertThat(converter.convert(event)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void convert_requestIdBlank_returnsEmptyString(String requestId) {

        given(event.getMDCPropertyMap()).willReturn(Map.of(MdcKeys.REQUEST_ID, requestId));

        assertThat(converter.convert(event)).isEmpty();
    }

    @Test
    void convert_fieldsMissing_usesUnknown() {

        given(event.getMDCPropertyMap()).willReturn(Map.of(MdcKeys.REQUEST_ID, "abc-123"));

        assertThat(converter.convert(event))
            .isEqualTo("[requestId=abc-123 method=- path=- accountId=-] ");
    }
}
