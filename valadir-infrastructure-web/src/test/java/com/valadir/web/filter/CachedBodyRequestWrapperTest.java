package com.valadir.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CachedBodyRequestWrapperTest {

    private static final String BODY = "{\"email\":\"bruce.wayne@email.com\"}";

    @Test
    void getInputStream_readTwice_returnsSameBodyBothTimes() throws IOException {

        var wrapper = wrapperFor(BODY);

        var firstRead = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var secondRead = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(firstRead).isEqualTo(BODY);
        assertThat(secondRead).isEqualTo(BODY);
    }

    @Test
    void getReader_nonAsciiBody_decodesAsUtf8() throws IOException {

        var wrapper = wrapperFor(BODY);

        try (var reader = wrapper.getReader()) {
            assertThat(reader.readLine()).isEqualTo(BODY);
        }
    }

    @Test
    void isFinished_beforeReading_returnsFalse() throws IOException {

        assertThat(wrapperFor(BODY).getInputStream().isFinished()).isFalse();
    }

    @Test
    void isFinished_afterConsumingBody_returnsTrue() throws IOException {

        var inputStream = wrapperFor(BODY).getInputStream();

        inputStream.readAllBytes();

        assertThat(inputStream.isFinished()).isTrue();
    }

    @Test
    void isFinished_emptyBody_returnsTrue() throws IOException {

        assertThat(wrapperFor("").getInputStream().isFinished()).isTrue();
    }

    @Test
    void isReady_always_returnsTrue() throws IOException {

        assertThat(wrapperFor(BODY).getInputStream().isReady()).isTrue();
    }

    @Test
    void setReadListener_asyncReadsUnsupported_throwsUnsupportedOperation() throws IOException {

        var inputStream = wrapperFor(BODY).getInputStream();

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> inputStream.setReadListener(null));
    }

    private CachedBodyRequestWrapper wrapperFor(String body) throws IOException {

        var request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new CachedBodyRequestWrapper(request);
    }
}
