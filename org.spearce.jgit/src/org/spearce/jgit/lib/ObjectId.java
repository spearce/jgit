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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * A SHA-1 abstraction.
 */
public class ObjectId implements Comparable {
	private static final ObjectId ZEROID;

	private static final String ZEROID_STR;

	static {
		ZEROID = new ObjectId(new byte[Constants.OBJECT_ID_LENGTH]);
		ZEROID_STR = ZEROID.toString();
	}

	/**
	 * @return an all zeroes ObjectId for special cases.
	 */
	public static ObjectId zeroId() {
		return ZEROID;
	}
	
	/**
	 * @param id
	 * @return true if id is a well formed hexadecimal SHA-1
	 */
	public static final boolean isId(final String id) {
		if (id.length() != (2 * Constants.OBJECT_ID_LENGTH)) {
			return false;
		}

		for (int k = id.length() - 1; k >= 0; k--) {
			final char c = id.charAt(k);
			if ('0' <= c && c <= '9') {
				continue;
			} else if ('a' <= c && c <= 'f') {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param i SHA-1
	 * @return the hexadecimal string representation of the supplied ObjectId
	 */
	public static String toString(final ObjectId i) {
		return i != null ? i.toString() : ZEROID_STR;
	}

	/**
	 * Construct an ObjectId from parts of a byte array
	 * @param i byte array
	 * @param offset within the byte array
	 * @return an ObjectId from the bits in the supplied array starting at offset
	 */
	public static ObjectId fromString(final byte[] i, int offset) {
		final byte[] id = new byte[Constants.OBJECT_ID_LENGTH];
		for (int k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final byte c1 = i[offset++];
			final byte c2 = i[offset++];
			int b;

			if ('0' <= c1 && c1 <= '9')
				b = c1 - '0';
			else if ('a' <= c1 && c1 <= 'f')
				b = c1 - 'a' + 10;
			else
				throw new IllegalArgumentException("Invalid id: " + (char) c1);

			b <<= 4;

			if ('0' <= c2 && c2 <= '9')
				b |= c2 - '0';
			else if ('a' <= c2 && c2 <= 'f')
				b |= c2 - 'a' + 10;
			else
				throw new IllegalArgumentException("Invalid id: " + (char) c2);
			id[k] = (byte) b;
		}
		return new ObjectId(id);
	}

	/**
	 * Compare this ObjectId with SHA-1 bytes of another id
	 *
	 * @param b
	 *            byte array containing SHA-1 bytes
	 * @param pos
	 *            offset of SHA-1 bytes
	 * @return 0 if the same, -1 or +1 de
	 */
	public int compareTo(byte[] b, long pos) {
		for (int k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final int ak = id[k] & 0xff;
			final int bk = b[k + (int)pos] & 0xff;
			if (ak < bk)
				return -1;
			else if (ak > bk)
				return 1;
		}
		return 0;
	}

	private static int compare(final byte[] a, final byte[] b) {
		for (int k = 0 ; k < Constants.OBJECT_ID_LENGTH; k++) {
			final int ak = a[k] & 0xff;
			final int bk = b[k] & 0xff;
			if (ak < bk)
				return -1;
			else if (ak > bk)
				return 1;
		}
		if (a.length != Constants.OBJECT_ID_LENGTH)
			throw new IllegalArgumentException("Looks like a bad object id");
		if (b.length != Constants.OBJECT_ID_LENGTH)
			throw new IllegalArgumentException("Looks like a bad object id");
		return 0;
	}

	private final byte[] id;

	/**
	 * Construct an ObjectId from a hexadecimal SHA-1
	 *
	 * @param i hexadecimal SHA-1
	 */
	public ObjectId(final String i) {
		if (i.length() != (2 * Constants.OBJECT_ID_LENGTH)) {
			throw new IllegalArgumentException("Invalid id \"" + i
					+ "\"; length = " + i.length());
		}

		id = new byte[Constants.OBJECT_ID_LENGTH];
		char[] bs = new char[Constants.OBJECT_ID_LENGTH*2];
		i.getChars(0,Constants.OBJECT_ID_LENGTH*2,bs,0);
		for (int j = 0, k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final char c1 = bs[j++];
			final char c2 = bs[j++];
			int b;

			if ('0' <= c1 && c1 <= '9') {
				b = c1 - '0';
			} else if ('a' <= c1 && c1 <= 'f') {
				b = c1 - 'a' + 10;
			} else {
				throw new IllegalArgumentException("Invalid id: " + i);
			}

			b <<= 4;

			if ('0' <= c2 && c2 <= '9') {
				b |= c2 - '0';
			} else if ('a' <= c2 && c2 <= 'f') {
				b |= c2 - 'a' + 10;
			} else {
				throw new IllegalArgumentException("Invalid id: " + i);
			}

			id[k] = (byte) b;
		}
	}

	/**
	 * Construct an ObjectId from SHA-1 bytes.
	 *
	 * @param i
	 */
	public ObjectId(final byte[] i) {
		id = i;
	}

	/**
	 * Helper for maps and set that use a 256 entry fan-out map
	 *
	 * @return as hash like value within the range 0..255
	 */
	public int getFirstByte() {
		return id[0] & 0xff;
	}

	/**
	 * @return the SHA-1 bytes
	 */
	public byte[] getBytes() {
		return id;
	}

	/**
	 * Compare the ObjectId to another SHA-1's bytes
	 *
	 * @param b
	 * @return -1,0 or +1 depending or recommended sort order.
	 */
	public int compareTo(final byte[] b) {
		return b != null ? compare(id, b) : 1;
	}

	/**
	 * Compare the ObjectId to another ObjectId
	 *
	 * @param b
	 * @return -1,0 or +1 depending or recommended sort order.
	 */
	public int compareTo(final ObjectId b) {
		return b != null ? compare(id, b.id) : 1;
	}

	public int compareTo(final Object o) {
		if (o instanceof ObjectId)
			return compare(id, ((ObjectId) o).id);
		if (o.getClass().getComponentType() == Byte.TYPE)
			return compare(id, (byte[]) o);
		return 1;
	}

	public int hashCode() {
		// Object Id' are well distributed so grab the first four bytes
		int b0 = id[0] & 0xff;
		int b1 = id[1] & 0xff;
		int b2 = id[2] & 0xff;
		int b3 = id[3] & 0xff;
		int h = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
		return h;
	}

	/**
	 * @param o
	 * @return true if this and the other object represent the same SHA-1
	 */
	public boolean equals(final ObjectId o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		for (int k = 0 ; k < Constants.OBJECT_ID_LENGTH; k++) {
			final byte ak = id[k];
			final byte bk = o.id[k];
			if (ak != bk)
				return false;
		}
		return true;
	}

	public boolean equals(final Object o) {
		return equals((ObjectId)o);
	}

	/**
	 * Write this ObjectId to a stream in hexadecimal format
	 * @param w
	 * @throws IOException
	 */
	public void copyTo(final OutputStream w) throws IOException {
		for (int k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final int b = id[k];
			final int b1 = (b >> 4) & 0xf;
			final int b2 = b & 0xf;
			w.write(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
			w.write(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
		}
	}

	/**
	 * Write this ObjectId to a Writer in hexadecimal format
	 * @param w
	 * @throws IOException
	 */
	public void copyTo(final Writer w) throws IOException {
		for (int k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final int b = id[k];
			final int b1 = (b >> 4) & 0xf;
			final int b2 = b & 0xf;
			w.write(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
			w.write(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
		}
	}

	public String toString() {
		final char s[] = new char[Constants.OBJECT_ID_LENGTH*2];
		int i = 0;
		for (int k = 0; k < Constants.OBJECT_ID_LENGTH; k++) {
			final int b = id[k];
			final int b1 = (b >> 4) & 0xf;
			final int b2 = b & 0xf;
			s[i++] = (b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
			s[i++] = (b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
		}
		return new String(s);
	}
}
