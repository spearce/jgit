/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.util;

/**
 * A rough character sequence around a raw byte buffer.
 * <p>
 * Characters are assumed to be 8-bit US-ASCII.
 */
public final class RawCharSequence implements CharSequence {
	/** A zero-length character sequence. */
	public static final RawCharSequence EMPTY = new RawCharSequence(null, 0, 0);

	final byte[] buffer;

	final int startPtr;

	final int endPtr;

	/**
	 * Create a rough character sequence around the raw byte buffer.
	 *
	 * @param buf
	 *            buffer to scan.
	 * @param start
	 *            starting position for the sequence.
	 * @param end
	 *            ending position for the sequence.
	 */
	public RawCharSequence(final byte[] buf, final int start, final int end) {
		buffer = buf;
		startPtr = start;
		endPtr = end;
	}

	public char charAt(final int index) {
		return (char) (buffer[startPtr + index] & 0xff);
	}

	public int length() {
		return endPtr - startPtr;
	}

	public CharSequence subSequence(final int start, final int end) {
		return new RawCharSequence(buffer, startPtr + start, startPtr + end);
	}

	@Override
	public String toString() {
		final int n = length();
		final StringBuilder b = new StringBuilder(n);
		for (int i = 0; i < n; i++)
			b.append(charAt(i));
		return b.toString();
	}
}