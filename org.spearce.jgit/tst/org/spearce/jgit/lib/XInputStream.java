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
import java.io.IOException;
import java.io.InputStream;

class XInputStream extends BufferedInputStream {
    private final byte[] intbuf = new byte[8];

    public XInputStream(final InputStream s) {
	super(s);
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
}
