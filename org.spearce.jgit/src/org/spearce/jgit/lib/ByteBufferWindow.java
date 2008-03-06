/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A window for accessing git packs using a {@link ByteBuffer} for storage.
 *
 * @see ByteWindow
 */
final class ByteBufferWindow extends ByteWindow<ByteBuffer> {
	/**
	 * Constructor.
	 *
	 * @See ByteWindow
	 *
	 * @param o The WindowedFile
	 * @param d Window id
	 * @param b ByteBuffer storage
	 */
	ByteBufferWindow(final WindowedFile o, final int d,
			final ByteBuffer b) {
		super(o, d, b, b.capacity());
	}

	final int copy(final ByteBuffer buffer, final int p, final byte[] b,
			final int o, int n) {
		final ByteBuffer s = buffer.slice();
		s.position(p);
		n = Math.min(s.remaining(), n);
		s.get(b, o, n);
		return n;
	}

	int inflate(final ByteBuffer buffer, final int pos, final byte[] b, int o,
			final Inflater inf)
			throws DataFormatException {
		final byte[] tmp = new byte[512];
		final ByteBuffer s = buffer.slice();
		s.position(pos);
		while (s.remaining() > 0 && !inf.finished()) {
			if (inf.needsInput()) {
				final int n = Math.min(s.remaining(), tmp.length);
				s.get(tmp, 0, n);
				inf.setInput(tmp, 0, n);
			}
			o += inf.inflate(b, o, b.length - o);
		}
		while (!inf.finished() && !inf.needsInput())
			o += inf.inflate(b, o, b.length - o);
		return o;
	}
}