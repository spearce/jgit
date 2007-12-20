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
public final class ByteBufferWindow extends ByteWindow {
	private final ByteBuffer buffer;

	/**
	 * Constructor.
	 *
	 * @See ByteWindow
	 *
	 * @param o The WindowProvider
	 * @param d Window id
	 * @param b ByteBuffer storage
	 */
	public ByteBufferWindow(final WindowProvider o, final int d,
			final ByteBuffer b) {
		super(o, d);
		buffer = b;
	}

	public final int copy(final int p, final byte[] b, final int o, int n) {
		final ByteBuffer s = buffer.slice();
		s.position(p);
		n = Math.min(s.remaining(), n);
		s.get(b, o, n);
		return n;
	}

	public int inflate(final int pos, final byte[] b, int o, final Inflater inf)
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

	public int size() {
		return buffer.capacity();
	}
}