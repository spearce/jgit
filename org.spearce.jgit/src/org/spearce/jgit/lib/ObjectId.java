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

import java.io.UnsupportedEncodingException;

import org.spearce.jgit.util.NB;

/**
 * A SHA-1 abstraction.
 */
public class ObjectId extends AnyObjectId {
	private static final ObjectId ZEROID;

	private static final String ZEROID_STR;

	static {
		ZEROID = new ObjectId(0, 0, 0, 0, 0);
		ZEROID_STR = ZEROID.toString();
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
		final int a = NB.decodeInt32(bs, p);
		final int b = NB.decodeInt32(bs, p + 4);
		final int c = NB.decodeInt32(bs, p + 8);
		final int d = NB.decodeInt32(bs, p + 12);
		final int e = NB.decodeInt32(bs, p + 16);
		return new ObjectId(a, b, c, d, e);
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

	protected ObjectId(final int new_1, final int new_2, final int new_3,
			final int new_4, final int new_5) {
		w1 = new_1;
		w2 = new_2;
		w3 = new_3;
		w4 = new_4;
		w5 = new_5;
	}

	/**
	 * Initialize this instance by copying another existing ObjectId.
	 * <p>
	 * This constructor is mostly useful for subclasses who want to extend an
	 * ObjectId with more properties, but initialize from an existing ObjectId
	 * instance acquired by other means.
	 * 
	 * @param src
	 *            another already parsed ObjectId to copy the value out of.
	 */
	protected ObjectId(final AnyObjectId src) {
		w1 = src.w1;
		w2 = src.w2;
		w3 = src.w3;
		w4 = src.w4;
		w5 = src.w5;
	}

	@Override
	public ObjectId toObjectId() {
		return this;
	}
}
