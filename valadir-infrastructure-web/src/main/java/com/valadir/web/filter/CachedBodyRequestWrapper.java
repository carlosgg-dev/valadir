package com.valadir.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Caches the request body so that it can be read multiple times.
 * Required when a filter reads the body before the controller does.
 */
final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    // The body is buffered before any limit has been consulted, so the cap is what bounds the heap
    // an unmetered request may claim. Every buffered route carries a handful of short JSON fields.
    static final int MAX_BODY_BYTES = 16 * 1024;

    private final byte[] cachedBody;

    CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {

        super(request);
        this.cachedBody = boundedBodyOf(request);
    }

    // The declared length is a claim, not a fact: it refuses the obvious case before a byte is read,
    // and the bounded read refuses a request that understates it rather than buffering what arrives.
    private static byte[] boundedBodyOf(HttpServletRequest request) throws IOException {

        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new RequestBodyTooLargeException(MAX_BODY_BYTES);
        }

        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new RequestBodyTooLargeException(MAX_BODY_BYTES);
        }

        return body;
    }

    @Override
    public ServletInputStream getInputStream() {

        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {

        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        CachedBodyServletInputStream(byte[] body) {

            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {

            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {

            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

            throw new UnsupportedOperationException("Async reads not supported");
        }

        @Override
        public int read() {

            return delegate.read();
        }
    }
}
