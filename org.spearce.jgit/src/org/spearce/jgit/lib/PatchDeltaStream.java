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

import java.io.IOException;
import java.io.InputStream;

import org.spearce.jgit.errors.CorruptObjectException;

/**
 * Recreate a stream from a base stream and a GIT pack delta.
 * <p>
 * This entire class is heavily cribbed from <code>patch-delta.c</code> in the
 * GIT project. The original delta patching code was written by Nicolas Pitre
 * (&lt;nico@cam.org&gt;).
 * </p>
 */
public class PatchDeltaStream extends InputStream {
    private final InputStream deltaStream;

    private byte[] base;

    private long resLen;

    private int cmd;

    private int copyOffset;

    private int copySize;

    public PatchDeltaStream(final InputStream delta,
	    final ObjectReader baseReader) throws IOException {
	long baseLen = 0;
	int shift;
	int c;

	deltaStream = delta;
	cmd = -1;

	// Length of the base object (a variable length int).
	//
	shift = 0;
	do {
	    c = readDelta();
	    baseLen |= (c & 0x7f) << shift;
	    shift += 7;
	} while ((c & 0x80) != 0);

	// Length of the resulting object (a variable length int).
	//
	shift = 0;
	do {
	    c = readDelta();
	    resLen |= (c & 0x7f) << shift;
	    shift += 7;
	} while ((c & 0x80) != 0);

	// We need to address parts of the base at random so pull the
	// entire base into a byte array to permit random accessing.
	//
	if (baseReader != null) {
	    final InputStream i = baseReader.getInputStream();
	    try {
		int expBaseLen = (int) baseReader.getSize();
		if (baseLen != expBaseLen) {
		    throw new CorruptObjectException("Base length from delta ("
			    + baseLen
			    + ") does not equal actual length from base ("
			    + expBaseLen + ").");
		}

		shift = 0;
		base = new byte[expBaseLen];
		while (expBaseLen > 0) {
		    final int n = i.read(base, shift, expBaseLen);
		    if (n < 0) {
			throw new CorruptObjectException("Can't completely"
				+ " load delta base for patching.");
		    }
		    shift += n;
		    expBaseLen -= n;
		}
	    } finally {
		i.close();
		baseReader.close();
	    }
	}
    }

    public long getResultLength() {
	return resLen;
    }

    public int read() throws IOException {
	final byte[] b = new byte[1];
	if (read(b, 0, 1) == 1) {
	    return b[0];
	} else {
	    return -1;
	}
    }

    public int read(final byte[] b, int off, int len) throws IOException {
	if (base == null) {
	    return -1;
	}

	int r = 0;
	while (len > 0) {
	    if (cmd == -1) {
		// We don't have a valid command ready so read the next
		// one from the delta stream.
		//
		cmd = deltaStream.read();
		if (cmd < 0) {
		    base = null;
		    return r;
		}

		if ((cmd & 0x80) != 0) {
		    // Determine the segment of the base which should
		    // be copied into the output. The segment is given
		    // as an offset and a length.
		    //
		    copyOffset = 0;
		    copySize = 0;

		    if ((cmd & 0x01) != 0) {
			copyOffset = readDelta();
		    }
		    if ((cmd & 0x02) != 0) {
			copyOffset |= readDelta() << 8;
		    }
		    if ((cmd & 0x04) != 0) {
			copyOffset |= readDelta() << 16;
		    }
		    if ((cmd & 0x08) != 0) {
			copyOffset |= readDelta() << 24;
		    }

		    if ((cmd & 0x10) != 0) {
			copySize = readDelta();
		    }
		    if ((cmd & 0x20) != 0) {
			copySize |= readDelta() << 8;
		    }
		    if ((cmd & 0x40) != 0) {
			copySize |= readDelta() << 16;
		    }

		    if (copySize == 0) {
			copySize = 0x10000;
		    }
		} else if (cmd != 0) {
		    // Anything else the data is literal within the delta
		    // itself.
		    //
		    copySize = cmd;
		} else {
		    // cmd == 0 has been reserved for future encoding but
		    // for now its not acceptable.
		    //
		    throw new CorruptObjectException("Delta is corrupt.");
		}
	    }

	    if ((cmd & 0x80) != 0) {
		// Copy the segment copyOffset through copyOffset+copySize
		// from the base to the output.
		//
		final int n = Math.min(len, copySize);
		System.arraycopy(base, copyOffset, b, off, n);
		r += n;
		off += n;
		len -= n;
		copySize -= n;
		if (copySize == 0) {
		    cmd = -1;
		} else {
		    copyOffset += n;
		}
	    } else if (cmd != 0) {
		// Literally copy from the delta to the output.
		//
		final int n = Math.min(len, copySize);
		xreadDelta(b, off, n);
		r += n;
		off += n;
		len -= n;
		copySize -= n;
		if (copySize == 0) {
		    cmd = -1;
		}
	    }
	}

	return r;
    }

    private int readDelta() throws IOException {
	final int r = deltaStream.read();
	if (r < 0) {
	    throw new CorruptObjectException("Unexpected end of delta.");
	}
	return r;
    }

    private void xreadDelta(final byte[] buf, int o, int len)
	    throws IOException {
	int r;
	while ((r = deltaStream.read(buf, o, len)) > 0) {
	    o += r;
	    len -= r;
	}
	if (len > 0) {
	    throw new CorruptObjectException("Unexpected end of delta.");
	}
    }

    public void close() throws IOException {
	deltaStream.close();
	base = null;
    }
}
