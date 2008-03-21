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
package org.spearce.jgit.lib;

import java.io.UnsupportedEncodingException;

/**
 * A mutable SHA-1 abstraction.
 */
public class MutableObjectId extends AnyObjectId {
	/**
	 * Convert an ObjectId from raw binary representation.
	 * 
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes must be
	 *            available within this byte array.
	 */
	public void fromRaw(final byte[] bs) {
		fromRaw(bs, 0);
	}

	/**
	 * Convert an ObjectId from raw binary representation.
	 * 
	 * @param bs
	 *            the raw byte buffer to read from. At least 20 bytes after p
	 *            must be available within this byte array.
	 * @param p
	 *            position to read the first byte of data from.
	 */
	public void fromRaw(final byte[] bs, final int p) {
		w1 = rawUInt32(bs, p);
		w2 = rawUInt32(bs, p + 4);
		w3 = rawUInt32(bs, p + 8);
		w4 = rawUInt32(bs, p + 12);
		w5 = rawUInt32(bs, p + 16);
	}

	/**
	 * Convert an ObjectId from hex characters (US-ASCII).
	 * 
	 * @param buf
	 *            the US-ASCII buffer to read from. At least 40 bytes after
	 *            offset must be available within this byte array.
	 * @param offset
	 *            position to read the first character from.
	 */
	public void fromString(final byte[] buf, final int offset) {
		fromHexString(buf, offset);
	}

	/**
	 * Convert an ObjectId from hex characters.
	 * 
	 * @param str
	 *            the string to read from. Must be 40 characters long.
	 */
	public void fromString(final String str) {
		if (str.length() != STR_LEN)
			throw new IllegalArgumentException("Invalid id: " + str);
		fromHexString(Constants.encodeASCII(str), 0);
	}

	private void fromHexString(final byte[] bs, int p) {
		try {
			w1 = hexUInt32(bs, p);
			w2 = hexUInt32(bs, p + 8);
			w3 = hexUInt32(bs, p + 16);
			w4 = hexUInt32(bs, p + 24);
			w5 = hexUInt32(bs, p + 32);
		} catch (ArrayIndexOutOfBoundsException e1) {
			try {
				final String str = new String(bs, p, STR_LEN, "US-ASCII");
				throw new IllegalArgumentException("Invalid id: " + str);
			} catch (UnsupportedEncodingException e2) {
				throw new IllegalArgumentException("Invalid id");
			}
		}
	}

	@Override
	public ObjectId toObjectId() {
		return new ObjectId(this);
	}
}
