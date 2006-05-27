package org.spearce.jgit.lib;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class XInputStream extends BufferedInputStream {
    private static final long uint8(final int i) {
        return ((i >> 4) & 0xf) << 4 | (i & 0xf);
    }

    private final byte[] uintbuf = new byte[8];

    private FileChannel fc;

    private long offset;

    public XInputStream(final FileInputStream s) {
        super(s);
        fc = s.getChannel();
    }

    public XInputStream(final InputStream s) {
        super(s);
    }

    public synchronized int read() throws IOException {
        final int n = super.read();
        if (n >= 0) {
            offset++;
        }
        return n;
    }

    public synchronized int read(final byte[] b, final int off, final int len)
            throws IOException {
        final int n = super.read(b, off, len);
        if (n >= 0) {
            offset += n;
        }
        return n;
    }

    public synchronized void reset() throws IOException {
        final int p = pos;
        super.reset();
        offset -= pos - p;
    }

    public synchronized long skip(final long n) throws IOException {
        final long r = super.skip(n);
        offset += r;
        return r;
    }

    public synchronized long position() throws IOException {
        return offset;
    }

    public synchronized void position(final long p) throws IOException {
        if (p != offset) {
            if (fc == null) {
                throw new IOException("Stream is not seekable.");
            }

            count = 0;
            offset = p;
            fc.position(p);
        }
    }

    public synchronized byte[] xread(final int len) throws IOException {
        final byte[] buf = new byte[len];
        xread(buf, 0, len);
        return buf;
    }

    public synchronized void xread(final byte[] buf, int o, int len)
            throws IOException {
        int r;
        while ((r = read(buf, o, len)) > 0) {
            o += r;
            len -= r;
        }
        if (len > 0) {
            throw new IOException("Unexpected end of stream.");
        }
    }

    public int xuint8() throws IOException {
        final int r = read();
        if (r < 0) {
            throw new IOException("Unexpected end of stream.");
        }
        return r;
    }

    public long xuint32() throws IOException {
        xread(uintbuf, 0, 4);
        return uint8(uintbuf[0]) << 24 | uint8(uintbuf[1]) << 16
                | uint8(uintbuf[2]) << 8 | uint8(uintbuf[3]);
    }

    public synchronized void close() throws IOException {
        fc = null;
        super.close();
    }
}
