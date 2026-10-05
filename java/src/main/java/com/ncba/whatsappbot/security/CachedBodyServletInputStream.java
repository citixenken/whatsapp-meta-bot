package com.ncba.whatsappbot.security;

import java.io.ByteArrayInputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/** Serves a cached request body as a re-readable {@link ServletInputStream}. */
class CachedBodyServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream buffer;

    CachedBodyServletInputStream(byte[] body) {
        this.buffer = new ByteArrayInputStream(body);
    }

    @Override
    public int read() {
        return buffer.read();
    }

    @Override
    public boolean isFinished() {
        return buffer.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
    }
}
