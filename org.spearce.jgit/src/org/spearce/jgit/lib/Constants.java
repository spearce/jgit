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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Constants {
	private static final String HASH_FUNCTION = "SHA-1";

	public static final int OBJECT_ID_LENGTH;

	public static final String HEAD = "HEAD";

	public static final String TYPE_COMMIT = "commit";

	public static final String TYPE_BLOB = "blob";

	public static final String TYPE_TREE = "tree";

	public static final String TYPE_TAG = "tag";

	public static final int OBJ_EXT = 0;

	public static final int OBJ_COMMIT = 1;

	public static final int OBJ_TREE = 2;

	public static final int OBJ_BLOB = 3;

	public static final int OBJ_TAG = 4;

	public static final int OBJ_TYPE_5 = 5;

	public static final int OBJ_OFS_DELTA = 6;

	public static final int OBJ_REF_DELTA = 7;

	public static final String CHARACTER_ENCODING = "UTF-8";

	public static MessageDigest newMessageDigest() {
		try {
			return MessageDigest.getInstance(HASH_FUNCTION);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException("Required hash function "
					+ HASH_FUNCTION + " not available.", nsae);
		}
	}

	public static byte[] encodeASCII(final long s) {
		return encodeASCII(Long.toString(s));
	}

	public static byte[] encodeASCII(final String s) {
		final byte[] r = new byte[s.length()];
		for (int k = r.length - 1; k >= 0; k--) {
			final char c = s.charAt(k);
			if (c > 127) {
				throw new IllegalArgumentException("Not ASCII string: " + s);
			}
			r[k] = (byte) c;
		}
		return r;
	}

	static {
		OBJECT_ID_LENGTH = newMessageDigest().getDigestLength();
	}

	private Constants() {
		// Hide the default constructor
	}
}
