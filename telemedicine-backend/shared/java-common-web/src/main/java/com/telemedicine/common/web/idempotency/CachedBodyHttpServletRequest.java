package com.telemedicine.common.web.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

/**
 * Buffers the request body so it can be read twice.
 *
 * <p>Spring's {@code ContentCachingRequestWrapper} records bytes as they are consumed but does not
 * replay them, so using it to fingerprint a body leaves the controller with an exhausted stream and
 * an empty payload. This wrapper reads the body up front and hands a fresh stream to every reader.
 */
class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    byte[] body() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new BufferedServletInputStream(new ByteArrayInputStream(body));
    }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), charset));
    }

    private static final class BufferedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        private BufferedServletInputStream(ByteArrayInputStream delegate) {
            this.delegate = delegate;
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
            throw new UnsupportedOperationException("Non-blocking reads are not supported here");
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            return delegate.read(buffer, offset, length);
        }

        @Override
        public int available() {
            return delegate.available();
        }
    }
}
