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

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A {@link ByteWindow} with an underlying byte array for storage.
 */
final class ByteArrayWindow extends ByteWindow<byte[]> {
	/**
	 * Constructor for ByteWindow.
	 * 
	 * @param o
	 *            the WindowedFile providing data access
	 * @param d
	 *            an id provided by the WindowedFile. See
	 *            {@link WindowCache#get(WindowCursor, WindowedFile, int)}.
	 * @param b
	 *            byte array for storage
	 */
	ByteArrayWindow(final WindowedFile o, final int d, final byte[] b) {
		super(o, d, b, b.length);
	}

	int copy(final byte[] array, final int p, final byte[] b, final int o, int n) {
		n = Math.min(array.length - p, n);
		System.arraycopy(array, p, b, o, n);
		return n;
	}

	int inflate(final byte[] array, final int pos, final byte[] b, int o,
			final Inflater inf) throws DataFormatException {
		while (!inf.finished()) {
			if (inf.needsInput()) {
				inf.setInput(array, pos, array.length - pos);
				break;
			}
			o += inf.inflate(b, o, b.length - o);
		}
		while (!inf.finished() && !inf.needsInput())
			o += inf.inflate(b, o, b.length - o);
		return o;
	}
}