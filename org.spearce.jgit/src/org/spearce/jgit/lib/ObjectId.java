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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A SHA-1 abstraction.
 */
public class ObjectId implements Comparable {
	private static final int RAW_LEN = Constants.OBJECT_ID_LENGTH;

	private static final int STR_LEN = RAW_LEN * 2;

	private static final ObjectId ZEROID;

	private static final String ZEROID_STR;

	private static final byte fromhex[];

	static {
		ZEROID = new ObjectId(0, 0, 0, 0, 0);
		ZEROID_STR = ZEROID.toString();

		fromhex = new byte['f' + 1];
		Arrays.fill(fromhex, (byte) -1);
		for (char i = '0'; i <= '9'; i++)
			fromhex[i] = (byte) (i - '0');
		for (char i = 'a'; i <= 'f'; i++)
			fromhex[i] = (byte) ((i - 'a') + 10);

		if (RAW_LEN != 20)
			throw new LinkageError("ObjectId expects"
					+ " Constants.OBJECT_ID_LENGTH = 20; it is " + RAW_LEN
					+ ".");
	}

	/**
	 * Get the special all-null ObjectId.
	 * 
	 * @return the all-null ObjectId, often used to stand-in for no object.
	 */
	public static final ObjectId zeroId() {
		return ZEROID;
	}

	/**
	 * Test a string of characters to verify it is a hex format.
	 * <p>
	 * If true the string can be parsed with {@link #fromString(String)}.
	 * 
	 * @param id
	 *            the string to test.
	 * @return true if the string can converted into an ObjectId.
	 */
	public static final boolean isId(final String id) {
		if (id.length() != 2 * Constants.OBJECT_ID_LENGTH)
			return false;
		try {
			for (int k = id.length() - 1; k >= 0; k--)
				if (fromhex[id.charAt(k)] < 0)
					return false;
			return true;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}

	/**
	 * Convert an ObjectId into a hex string representation.
	 * 
	 * @param i
	 *            the id to convert. May be null.
	 * @return the hex string conversion of this id's content.
	 */
	public static final String toString(final ObjectId i) {
		return i != null ? i.toString() : ZEROID_STR;
	}

	/**
	 * Compare to object identifier byte sequences for equality.
	 * 
	 * @param firstBuffer
	 *            the first buffer to compare against. Must have at least 20
	 *            bytes from position ai through the end of the buffer.
	 * @param fi
	 *            first offset within firstBuffer to begin testing.
	 * @param secondBuffer
	 *            the second buffer to compare against. Must have at least 2
	 *            bytes from position bi through the end of the buffer.
	 * @param si
	 *            first offset within secondBuffer to begin testing.
	 * @return true if the two identifiers are the same.
	 */
	public static boolean equals(final byte[] firstBuffer, final int fi,
			final byte[] secondBuffer, final int si) {
		return firstBuffer[fi] == secondBuffer[si]
				&& firstBuffer[fi + 1] == secondBuffer[si + 1]
				&& firstBuffer[fi + 2] == secondBuffer[si + 2]
				&& firstBuffer[fi + 3] == secondBuffer[si + 3]
				&& firstBuffer[fi + 4] == secondBuffer[si + 4]
				&& firstBuffer[fi + 5] == secondBuffer[si + 5]
				&& firstBuffer[fi + 6] == secondBuffer[si + 6]
				&& firstBuffer[fi + 7] == secondBuffer[si + 7]
				&& firstBuffer[fi + 8] == secondBuffer[si + 8]
				&& firstBuffer[fi + 9] == secondBuffer[si + 9]
				&& firstBuffer[fi + 10] == secondBuffer[si + 10]
				&& firstBuffer[fi + 11] == secondBuffer[si + 11]
				&& firstBuffer[fi + 12] == secondBuffer[si + 12]
				&& firstBuffer[fi + 13] == secondBuffer[si + 13]
				&& firstBuffer[fi + 14] == secondBuffer[si + 14]
				&& firstBuffer[fi + 15] == secondBuffer[si + 15]
				&& firstBuffer[fi + 16] == secondBuffer[si + 16]
				&& firstBuffer[fi + 17] == secondBuffer[si + 17]
				&& firstBuffer[fi + 18] == secondBuffer[si + 18]
				&& firstBuffer[fi + 19] == secondBuffer[si + 19];
	}

	/**
	 * Compare to object identifier byte sequences for equality.
	 * 
	 * @param firstObjectId
	 *            the first identifier to compare. Must not be null.
	 * @param secondObjectId
	 *            the second identifier to compare. Must not be null.
	 * @return true if the two identifiers are the same.
	 */
	public static boolean equals(final ObjectId firstObjectId,
			final ObjectId secondObjectId) {
		if (firstObjectId == secondObjectId)
			return true;

		// We test word 2 first as odds are someone already used our
		// word 1 as a hash code, and applying that came up with these
		// two instances we are comparing for equality. Therefore the
		// first two words are very likely to be identical. We want to
		// break away from collisions as quickly as possible.
		//
		return firstObjectId.w2 == secondObjectId.w2
				&& firstObjectId.w3 == secondObjectId.w3
				&& firstObjectId.w4 == secondObjectId.w4
				&& firstObjectId.w5 == secondObjectId.w5
				&& firstObjectId.w1 == secondObjectId.w1;
	}

	/**
	 * Convert an ObjectId from raw binary representation.
	 * 
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes must be
	 *            available within this byte array.
	 * @return the converted object id.
	 */
	public static final ObjectId fromRaw(final byte[] bs) {
		return fromRaw(bs, 0);
	}

	/**
	 * Convert an ObjectId from raw binary representation.
	 * 
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes after p
	 *            must be available within this byte array.
	 * @param p
	 *            position to read the first byte of data from.
	 * @return the converted object id.
	 */
	public static final ObjectId fromRaw(final byte[] bs, final int p) {
		final int a = rawUInt32(bs, p);
		final int b = rawUInt32(bs, p + 4);
		final int c = rawUInt32(bs, p + 8);
		final int d = rawUInt32(bs, p + 12);
		final int e = rawUInt32(bs, p + 16);
		return new ObjectId(a, b, c, d, e);
	}

	static final int rawUInt32(final byte[] rawBuffer, int offset) {
		int r = rawBuffer[offset] << 8;

		r |= rawBuffer[offset + 1] & 0xff;
		r <<= 8;

		r |= rawBuffer[offset + 2] & 0xff;
		return (r << 8) | (rawBuffer[offset + 3] & 0xff);
	}

	/**
	 * Convert an ObjectId from hex characters (US-ASCII).
	 * 
	 * @param buf
	 *            the US-ASCII buffer to read from. At least 40 bytes after
	 *            offset must be available within this byte array.
	 * @param offset
	 *            position to read the first character from.
	 * @return the converted object id.
	 */
	public static final ObjectId fromString(final byte[] buf, final int offset) {
		return fromHexString(buf, offset);
	}

	/**
	 * Convert an ObjectId from hex characters.
	 * 
	 * @param str
	 *            the string to read from. Must be 40 characters long.
	 * @return the converted object id.
	 */
	public static final ObjectId fromString(final String str) {
		if (str.length() != STR_LEN)
			throw new IllegalArgumentException("Invalid id: " + str);
		return fromHexString(Constants.encodeASCII(str), 0);
	}

	private static final ObjectId fromHexString(final byte[] bs, int p) {
		try {
			final int a = hexUInt32(bs, p);
			final int b = hexUInt32(bs, p + 8);
			final int c = hexUInt32(bs, p + 16);
			final int d = hexUInt32(bs, p + 24);
			final int e = hexUInt32(bs, p + 32);
			return new ObjectId(a, b, c, d, e);
		} catch (ArrayIndexOutOfBoundsException e1) {
			try {
				final String str = new String(bs, p, STR_LEN, "US-ASCII");
				throw new IllegalArgumentException("Invalid id: " + str);
			} catch (UnsupportedEncodingException e2) {
				throw new IllegalArgumentException("Invalid id");
			}
		}
	}

	private static final int hexUInt32(final byte[] bs, final int p) {
		int r = fromhex[bs[p]] << 4;

		r |= fromhex[bs[p + 1]];
		r <<= 4;

		r |= fromhex[bs[p + 2]];
		r <<= 4;

		r |= fromhex[bs[p + 3]];
		r <<= 4;

		r |= fromhex[bs[p + 4]];
		r <<= 4;

		r |= fromhex[bs[p + 5]];
		r <<= 4;

		r |= fromhex[bs[p + 6]];

		final int last = fromhex[bs[p + 7]];
		if (r < 0 || last < 0)
			throw new ArrayIndexOutOfBoundsException();
		return (r << 4) | last;
	}

	final int w1;

	final int w2;

	final int w3;

	final int w4;

	final int w5;

	private ObjectId(final int new_1, final int new_2, final int new_3,
			final int new_4, final int new_5) {
		w1 = new_1;
		w2 = new_2;
		w3 = new_3;
		w4 = new_4;
		w5 = new_5;
	}

	final int getFirstByte() {
		return w1 >>> 24;
	}

	/**
	 * Compare this ObjectId to another and obtain a sort ordering.
	 * 
	 * @param other
	 *            the other id to compare to. Must not be null.
	 * @return < 0 if this id comes before other; 0 if this id is equal to
	 *         other; > 0 if this id comes after other.
	 */
	public int compareTo(final ObjectId other) {
		if (this == other)
			return 0;

		int cmp;

		cmp = compareUInt32(w1, other.w1);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w2, other.w2);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w3, other.w3);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w4, other.w4);
		if (cmp != 0)
			return cmp;

		return compareUInt32(w5, other.w5);
	}

	public int compareTo(final Object other) {
		return compareTo(((ObjectId) other));
	}

	int compareTo(final byte[] bs, final int p) {
		int cmp;

		cmp = compareUInt32(w1, rawUInt32(bs, p));
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w2, rawUInt32(bs, p + 4));
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w3, rawUInt32(bs, p + 8));
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w4, rawUInt32(bs, p + 12));
		if (cmp != 0)
			return cmp;

		return compareUInt32(w5, rawUInt32(bs, p + 16));
	}

	int compareTo(final int[] bs, final int p) {
		int cmp;

		cmp = compareUInt32(w1, bs[p]);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w2, bs[p + 1]);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w3, bs[p + 2]);
		if (cmp != 0)
			return cmp;

		cmp = compareUInt32(w4, bs[p + 3]);
		if (cmp != 0)
			return cmp;

		return compareUInt32(w5, bs[p + 4]);
	}

	private static final int compareUInt32(final int a, final int b) {
		final int cmp = (a >>> 1) - (b >>> 1);
		if (cmp != 0)
			return cmp;
		return (a & 1) - (b & 1);
	}

	public int hashCode() {
		return w2;
	}

	/**
	 * Determine if this ObjectId has exactly the same value as another.
	 * 
	 * @param other
	 *            the other id to compare to. May be null.
	 * @return true only if both ObjectIds have identical bits.
	 */
	public boolean equals(final ObjectId other) {
		return other != null ? equals(this, other) : false;
	}

	public boolean equals(final Object o) {
		return equals((ObjectId) o);
	}

	/**
	 * Copy this ObjectId to an output writer in raw binary.
	 * 
	 * @param w
	 *            the buffer to copy to. Must be in big endian order.
	 */
	public void copyRawTo(final ByteBuffer w) {
		w.putInt(w1);
		w.putInt(w2);
		w.putInt(w3);
		w.putInt(w4);
		w.putInt(w5);
	}

	/**
	 * Copy this ObjectId to an output writer in raw binary.
	 * 
	 * @param w
	 *            the stream to write to.
	 * @throws IOException
	 *             the stream writing failed.
	 */
	public void copyRawTo(final OutputStream w) throws IOException {
		writeRawInt(w, w1);
		writeRawInt(w, w2);
		writeRawInt(w, w3);
		writeRawInt(w, w4);
		writeRawInt(w, w5);
	}

	private static void writeRawInt(final OutputStream w, int v)
			throws IOException {
		w.write(v >>> 24);
		w.write(v >>> 16);
		w.write(v >>> 8);
		w.write(v);
	}

	/**
	 * Copy this ObjectId to an output writer in hex format.
	 * 
	 * @param w
	 *            the stream to copy to.
	 * @throws IOException
	 *             the stream writing failed.
	 */
	public void copyTo(final OutputStream w) throws IOException {
		w.write(toHexByteArray());
	}

	private byte[] toHexByteArray() {
		final byte[] dst = new byte[STR_LEN];
		formatHexByte(dst, 0, w1);
		formatHexByte(dst, 8, w2);
		formatHexByte(dst, 16, w3);
		formatHexByte(dst, 24, w4);
		formatHexByte(dst, 32, w5);
		return dst;
	}

	private static final byte[] hexbyte = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static void formatHexByte(final byte[] dst, final int p, int w) {
		int o = p + 7;
		while (o >= p && w != 0) {
			dst[o--] = hexbyte[w & 0xf];
			w >>>= 4;
		}
		while (o >= p)
			dst[o--] = '0';
	}

	/**
	 * Copy this ObjectId to an output writer in hex format.
	 * 
	 * @param w
	 *            the stream to copy to.
	 * @throws IOException
	 *             the stream writing failed.
	 */
	public void copyTo(final Writer w) throws IOException {
		w.write(toHexCharArray());
	}

	private char[] toHexCharArray() {
		final char[] dst = new char[STR_LEN];
		formatHexChar(dst, 0, w1);
		formatHexChar(dst, 8, w2);
		formatHexChar(dst, 16, w3);
		formatHexChar(dst, 24, w4);
		formatHexChar(dst, 32, w5);
		return dst;
	}

	private static final char[] hexchar = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static void formatHexChar(final char[] dst, final int p, int w) {
		int o = p + 7;
		while (o >= p && w != 0) {
			dst[o--] = hexchar[w & 0xf];
			w >>>= 4;
		}
		while (o >= p)
			dst[o--] = '0';
	}

	public String toString() {
		return new String(toHexCharArray());
	}
}
