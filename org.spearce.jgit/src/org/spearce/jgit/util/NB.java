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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** Conversion utilities for network byte order handling. */
public final class NB {
	/**
	 * Read the entire byte array into memory, or throw an exception.
	 * 
	 * @param fd
	 *            input stream to read the data from.
	 * @param dst
	 *            buffer that must be fully populated, [off, off+len).
	 * @param off
	 *            position within the buffer to start writing to.
	 * @param len
	 *            number of bytes that must be read.
	 * @throws EOFException
	 *             the stream ended before dst was fully populated.
	 * @throws IOException
	 *             there was an error reading from the stream.
	 */
	public static void readFully(final InputStream fd, final byte[] dst,
			int off, int len) throws IOException {
		while (len > 0) {
			final int r = fd.read(dst, off, len);
			if (r <= 0)
				throw new EOFException("Short read of block.");
			off += r;
			len -= r;
		}
	}

	/**
	 * Compare a 32 bit unsigned integer stored in a 32 bit signed integer.
	 * <p>
	 * This function performs an unsigned compare operation, even though Java
	 * does not natively support unsigned integer values. Negative numbers are
	 * treated as larger than positive ones.
	 * 
	 * @param a
	 *            the first value to compare.
	 * @param b
	 *            the second value to compare.
	 * @return < 0 if a < b; 0 if a == b; > 0 if a > b.
	 */
	public static int compareUInt32(final int a, final int b) {
		final int cmp = (a >>> 1) - (b >>> 1);
		if (cmp != 0)
			return cmp;
		return (a & 1) - (b & 1);
	}

	/**
	 * Convert sequence of 4 bytes (network byte order) into signed value.
	 * 
	 * @param intbuf
	 *            buffer to acquire the 4 bytes of data from.
	 * @param offset
	 *            position within the buffer to begin reading from. This
	 *            position and the next 3 bytes after it (for a total of 4
	 *            bytes) will be read.
	 * @return signed integer value that matches the 32 bits read.
	 */
	public static int decodeInt32(final byte[] intbuf, final int offset) {
		int r = intbuf[offset] << 8;

		r |= intbuf[offset + 1] & 0xff;
		r <<= 8;

		r |= intbuf[offset + 2] & 0xff;
		return (r << 8) | (intbuf[offset + 3] & 0xff);
	}

	/**
	 * Convert sequence of 4 bytes (network byte order) into unsigned value.
	 * 
	 * @param intbuf
	 *            buffer to acquire the 4 bytes of data from.
	 * @param offset
	 *            position within the buffer to begin reading from. This
	 *            position and the next 3 bytes after it (for a total of 4
	 *            bytes) will be read.
	 * @return unsigned integer value that matches the 32 bits read.
	 */
	public static long decodeUInt32(final byte[] intbuf, final int offset) {
		int low = (intbuf[offset + 1] & 0xff) << 8;
		low |= (intbuf[offset + 2] & 0xff);
		low <<= 8;

		low |= (intbuf[offset + 3] & 0xff);
		return ((long) (intbuf[offset] & 0xff)) << 24 | low;
	}

	/**
	 * Convert sequence of 8 bytes (network byte order) into unsigned value.
	 * 
	 * @param intbuf
	 *            buffer to acquire the 8 bytes of data from.
	 * @param offset
	 *            position within the buffer to begin reading from. This
	 *            position and the next 7 bytes after it (for a total of 8
	 *            bytes) will be read.
	 * @return unsigned integer value that matches the 64 bits read.
	 */
	public static long decodeUInt64(final byte[] intbuf, final int offset) {
		return (decodeUInt32(intbuf, offset) << 32)
				| decodeUInt32(intbuf, offset + 4);
	}

	/**
	 * Write a 32 bit integer as a sequence of 4 bytes (network byte order).
	 * 
	 * @param intbuf
	 *            buffer to write the 4 bytes of data into.
	 * @param offset
	 *            position within the buffer to begin writing to. This position
	 *            and the next 3 bytes after it (for a total of 4 bytes) will be
	 *            replaced.
	 * @param v
	 *            the value to write.
	 */
	public static void encodeInt32(final byte[] intbuf, final int offset, int v) {
		intbuf[offset + 3] = (byte) v;
		v >>>= 8;

		intbuf[offset + 2] = (byte) v;
		v >>>= 8;

		intbuf[offset + 1] = (byte) v;
		v >>>= 8;

		intbuf[offset] = (byte) v;
	}

	private NB() {
		// Don't create instances of a static only utility.
	}
}
