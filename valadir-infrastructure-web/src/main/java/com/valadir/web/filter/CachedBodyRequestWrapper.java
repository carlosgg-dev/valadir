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
class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    CachedBodyRequestWrapper(final HttpServletRequest request) throws IOException {

        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
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

        CachedBodyServletInputStream(final byte[] body) {

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
        public void setReadListener(final ReadListener readListener) {

            throw new UnsupportedOperationException("Async reads not supported");
        }

        @Override
        public int read() {

            return delegate.read();
        }
    }
}
