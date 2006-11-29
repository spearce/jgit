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
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

abstract class PackedObjectReader extends ObjectReader {
    protected final PackFile pack;

    protected final long dataOffset;

    protected String objectType;

    protected long objectSize;

    protected PackedObjectReader(final PackFile pr, final long offset) {
	pack = pr;
	dataOffset = offset;
    }

    public String getType() throws IOException {
	return objectType;
    }

    public long getSize() throws IOException {
	return objectSize;
    }

    public long getDataOffset() {
	return dataOffset;
    }

    public InputStream getInputStream() throws IOException {
	return packStream();
    }

    public void close() throws IOException {
    }

    protected BufferedInputStream packStream() {
	return new BufferedInputStream(new PackStream());
    }

    private class PackStream extends InputStream {
	private Inflater inf = new Inflater(false);

	private byte[] in = new byte[2048];

	private long offset = getDataOffset();

	public int read() throws IOException {
	    final byte[] sbb = new byte[1];
	    return read(sbb, 0, 1) == 1 ? sbb[0] & 0xff : -1;
	}

	public int read(final byte[] b, final int off, final int len)
		throws IOException {
	    if (inf.finished()) {
		return -1;
	    }

	    if (inf.needsInput()) {
		final int n = pack.read(offset, in, 0, in.length);
		inf.setInput(in, 0, n);
		offset += n;
	    }

	    try {
		return inf.inflate(b, off, len);
	    } catch (DataFormatException dfe) {
		final IOException e = new IOException("Corrupt ZIP stream.");
		e.initCause(dfe);
		throw e;
	    }
	}

	public void close() {
	    if (inf != null) {
		inf.end();
		inf = null;
		in = null;
	    }
	}
    }
}
