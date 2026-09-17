package com.valadir.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.valadir.web.filter.CachedBodyRequestWrapper.MAX_BODY_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CachedBodyRequestWrapperTest {

    private static final String BODY = "{\"email\":\"bruce.wayne@email.com\"}";

    // Transfer-Encoding: chunked — what getContentLengthLong reports when no Content-Length arrives
    private static final long UNDECLARED_LENGTH = -1;

    @Test
    void getInputStream_readTwice_returnsSameBodyBothTimes() throws IOException {

        var wrapper = wrapperOf(BODY);

        var firstRead = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var secondRead = new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(firstRead).isEqualTo(BODY);
        assertThat(secondRead).isEqualTo(BODY);
    }

    @Test
    void getReader_nonAsciiBody_decodesAsUtf8() throws IOException {

        var wrapper = wrapperOf(BODY);

        try (var reader = wrapper.getReader()) {
            assertThat(reader.readLine()).isEqualTo(BODY);
        }
    }

    @Test
    void isFinished_beforeReading_returnsFalse() throws IOException {

        assertThat(wrapperOf(BODY).getInputStream().isFinished()).isFalse();
    }

    @Test
    void isFinished_afterConsumingBody_returnsTrue() throws IOException {

        var inputStream = wrapperOf(BODY).getInputStream();

        inputStream.readAllBytes();

        assertThat(inputStream.isFinished()).isTrue();
    }

    @Test
    void isFinished_emptyBody_returnsTrue() throws IOException {

        assertThat(wrapperOf("").getInputStream().isFinished()).isTrue();
    }

    @Test
    void isReady_always_returnsTrue() throws IOException {

        assertThat(wrapperOf(BODY).getInputStream().isReady()).isTrue();
    }

    @Test
    void setReadListener_asyncReadsUnsupported_throwsUnsupportedOperation() throws IOException {

        var inputStream = wrapperOf(BODY).getInputStream();

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> inputStream.setReadListener(null));
    }

    @Test
    void constructor_declaredLengthAtTheLimit_cachesIt() throws IOException {

        var request = requestDeclaring(MAX_BODY_BYTES, new byte[MAX_BODY_BYTES]);

        var wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getInputStream().readAllBytes()).hasSize(MAX_BODY_BYTES);
    }

    // The body is short, so the bounded read alone would accept it: only the declared length can refuse this one
    @Test
    void constructor_declaredLengthAboveTheLimit_isRejectedBeforeTheBodyIsRead() {

        var request = requestDeclaring(MAX_BODY_BYTES + 1, BODY.getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(RequestBodyTooLargeException.class)
            .isThrownBy(() -> new CachedBodyRequestWrapper(request));
    }

    @Test
    void constructor_undeclaredLengthWithABodyAtTheLimit_cachesIt() throws IOException {

        var request = requestDeclaring(UNDECLARED_LENGTH, new byte[MAX_BODY_BYTES]);

        var wrapper = new CachedBodyRequestWrapper(request);

        assertThat(wrapper.getInputStream().readAllBytes()).hasSize(MAX_BODY_BYTES);
    }

    // Nothing is declared, so only the bounded read can refuse it
    @Test
    void constructor_undeclaredLengthWithABodyAboveTheLimit_isRejectedOnTheRead() {

        var request = requestDeclaring(UNDECLARED_LENGTH, new byte[MAX_BODY_BYTES + 1]);

        assertThatExceptionOfType(RequestBodyTooLargeException.class)
            .isThrownBy(() -> new CachedBodyRequestWrapper(request));
    }

    private CachedBodyRequestWrapper wrapperOf(String body) throws IOException {

        var request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new CachedBodyRequestWrapper(request);
    }

    // What a request declares and what it sends are two independent values, and MockHttpServletRequest
    // derives the first from the second — so the declared length is overridden to tell them apart
    private static MockHttpServletRequest requestDeclaring(long declaredLength, byte[] body) {

        var request = new MockHttpServletRequest() {

            @Override
            public long getContentLengthLong() {

                return declaredLength;
            }
        };

        request.setContent(body);
        return request;
    }
}
