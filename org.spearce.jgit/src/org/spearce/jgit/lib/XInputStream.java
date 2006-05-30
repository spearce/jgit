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

    private final byte[] intbuf = new byte[8];

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
        // BufferedInputStream doesn't skip until its buffer is empty so we
        // might need to invoke skip more than once until all underlying
        // InputStreams have advanced by the amount requested.
        //
        long r = 0;
        while (r < n) {
            final long i = super.skip(n - r);
            if (i <= 0) {
                break;
            }
            r += i;
        }
        offset += r;
        return r;
    }

    public synchronized long position() throws IOException {
        return offset;
    }

    public synchronized void position(final long p) throws IOException {
        if (p < offset) {
            final long d = offset - p;
            if (d < pos) {
                // Rewind target is within the buffer. We can simply reset to
                // it by altering pos.
                //
                offset = p;
                pos -= d;
            } else {
                // Rewind target isn't in the buffer; we need to rewind the
                // stream and clear the buffer. We can only do this if it has
                // a FileChannel as otherwise we have no way to position it.
                //
                if (fc == null) {
                    throw new IOException("Stream is not reverse seekable.");
                }
                count = 0;
                offset = p;
                fc.position(p);
            }
        } else if (p > offset) {
            // Fast-foward is simply a skip and most streams are skippable even
            // if skipping would require consuming all data to actually perform
            // the skip. Verify the skip took place because if it didn't then
            // things didn't work as we had planned (the stream isn't seekable
            // or we are trying to position past the end in an effor to create a
            // hole, which we don't really support doing here).
            //
            skip(p - offset);
            if (offset != p) {
                throw new IOException("Stream is not forward seekable.");
            }
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
        xread(intbuf, 0, 4);
        return uint8(intbuf[0]) << 24 | uint8(intbuf[1]) << 16
                | uint8(intbuf[2]) << 8 | uint8(intbuf[3]);
    }

    public synchronized void close() throws IOException {
        fc = null;
        super.close();
    }
}
