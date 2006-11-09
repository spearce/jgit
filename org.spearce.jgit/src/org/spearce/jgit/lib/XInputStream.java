/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class XInputStream extends BufferedInputStream {
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
	offset -= p - pos;
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
	} else if (p > offset) {
	    // Fast-foward is simply a skip and most streams are skippable
                // even
	    // if skipping would require consuming all data to actually
                // perform
	    // the skip. Verify the skip took place because if it didn't
                // then
	    // things didn't work as we had planned (the stream isn't
                // seekable
	    // or we are trying to position past the end in an effor to
                // create a
	    // hole, which we don't really support doing here).
	    //
	    skip(p - offset);
	    if (offset != p) {
		throw new IOException("Stream is not forward seekable.");
	    }
	}
    }

    public synchronized byte[] readFully(final int len) throws IOException {
	final byte[] buf = new byte[len];
	readFully(buf, 0, len);
	return buf;
    }

    public synchronized void readFully(final byte[] buf, int o, int len)
	    throws IOException {
	int r;
	while (len > 0 && (r = read(buf, o, len)) > 0) {
	    o += r;
	    len -= r;
	}
	if (len > 0) {
	    throw new EOFException();
	}
    }

    public int readUInt8() throws IOException {
	final int r = read();
	if (r < 0) {
	    throw new EOFException();
	}
	return r;
    }

    public long readUInt32() throws IOException {
	readFully(intbuf, 0, 4);
	return (intbuf[0] & 0xff) << 24 | (intbuf[1] & 0xff) << 16
		| (intbuf[2] & 0xff) << 8 | (intbuf[3] & 0xff);
    }

    public synchronized void close() throws IOException {
	fc = null;
	super.close();
    }
}
