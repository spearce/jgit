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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Misc. constants used throughout JGit. */
public final class Constants {
	/** Hash function used natively by Git for all objects. */
	private static final String HASH_FUNCTION = "SHA-1";

	/** Length of an object hash. */
	public static final int OBJECT_ID_LENGTH = 20;

	/** Special name for the "HEAD" symbolic-ref. */
	public static final String HEAD = "HEAD";

	/**
	 * Text string that identifies an object as a commit.
	 * <p>
	 * Commits connect trees into a string of project histories, where each
	 * commit is an assertion that the best way to continue is to use this other
	 * tree (set of files).
	 */
	public static final String TYPE_COMMIT = "commit";

	/**
	 * Text string that identifies an object as a blob.
	 * <p>
	 * Blobs store whole file revisions. They are used for any user file, as
	 * well as for symlinks. Blobs form the bulk of any project's storage space.
	 */
	public static final String TYPE_BLOB = "blob";

	/**
	 * Text string that identifies an object as a tree.
	 * <p>
	 * Trees attach object ids (hashes) to names and file modes. The normal use
	 * for a tree is to store a version of a directory and its contents.
	 */
	public static final String TYPE_TREE = "tree";

	/**
	 * Text string that identifies an object as an annotated tag.
	 * <p>
	 * Annotated tags store a pointer to any other object, and an additional
	 * message. It is most commonly used to record a stable release of the
	 * project.
	 */
	public static final String TYPE_TAG = "tag";

	/** An unknown or invalid object type code. */
	public static final int OBJ_BAD = -1;

	/**
	 * In-pack object type: extended types.
	 * <p>
	 * This header code is reserved for future expansion. It is currently
	 * undefined/unsupported.
	 */
	public static final int OBJ_EXT = 0;

	/**
	 * In-pack object type: commit.
	 * <p>
	 * Indicates the associated object is a commit.
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 * 
	 * @see #TYPE_COMMIT
	 */
	public static final int OBJ_COMMIT = 1;

	/**
	 * In-pack object type: tree.
	 * <p>
	 * Indicates the associated object is a tree.
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 * 
	 * @see #TYPE_BLOB
	 */
	public static final int OBJ_TREE = 2;

	/**
	 * In-pack object type: blob.
	 * <p>
	 * Indicates the associated object is a blob.
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 * 
	 * @see #TYPE_BLOB
	 */
	public static final int OBJ_BLOB = 3;

	/**
	 * In-pack object type: annotated tag.
	 * <p>
	 * Indicates the associated object is an annotated tag.
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 * 
	 * @see #TYPE_TAG
	 */
	public static final int OBJ_TAG = 4;

	/** In-pack object type: reserved for future use. */
	public static final int OBJ_TYPE_5 = 5;

	/**
	 * In-pack object type: offset delta
	 * <p>
	 * Objects stored with this type actually have a different type which must
	 * be obtained from their delta base object. Delta objects store only the
	 * changes needed to apply to the base object in order to recover the
	 * original object.
	 * <p>
	 * An offset delta uses a negative offset from the start of this object to
	 * refer to its delta base. The base object must exist in this packfile
	 * (even in the case of a thin pack).
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 */
	public static final int OBJ_OFS_DELTA = 6;

	/**
	 * In-pack object type: reference delta
	 * <p>
	 * Objects stored with this type actually have a different type which must
	 * be obtained from their delta base object. Delta objects store only the
	 * changes needed to apply to the base object in order to recover the
	 * original object.
	 * <p>
	 * A reference delta uses a full object id (hash) to reference the delta
	 * base. The base object is allowed to be omitted from the packfile, but
	 * only in the case of a thin pack being transferred over the network.
	 * <p>
	 * <b>This constant is fixed and is defined by the Git packfile format.</b>
	 */
	public static final int OBJ_REF_DELTA = 7;

	/** Native character encoding for commit messages, file names... */
	public static final String CHARACTER_ENCODING = "UTF-8";

	/** Native character encoding for commit messages, file names... */
	public static final Charset CHARSET;

	/** Default main branch name */
	public static final String MASTER = "master";

	/** Prefix for branch refs */
	public static final String HEADS_PREFIX = "refs/heads";

	/** Prefix for remotes refs */
	public static final String REMOTES_PREFIX = "refs/remotes";

	/**
	 * Create a new digest function for objects.
	 * 
	 * @return a new digest object.
	 * @throws RuntimeException
	 *             this Java virtual machine does not support the required hash
	 *             function. Very unlikely given that JGit uses a hash function
	 *             that is in the Java reference specification.
	 */
	public static MessageDigest newMessageDigest() {
		try {
			return MessageDigest.getInstance(HASH_FUNCTION);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException("Required hash function "
					+ HASH_FUNCTION + " not available.", nsae);
		}
	}

	/**
	 * Convert an OBJ_* type constant to a TYPE_* type constant.
	 *
	 * @param typeCode the type code, from a pack representation.
	 * @return the canonical string name of this type.
	 */
	public static String typeString(final int typeCode) {
		switch (typeCode) {
		case OBJ_COMMIT:
			return TYPE_COMMIT;
		case OBJ_TREE:
			return TYPE_TREE;
		case OBJ_BLOB:
			return TYPE_BLOB;
		case OBJ_TAG:
			return TYPE_TAG;
		default:
			throw new IllegalArgumentException("Bad object type: " + typeCode);
		}
	}

	/**
	 * Convert an integer into its decimal representation.
	 * 
	 * @param s
	 *            the integer to convert.
	 * @return a decimal representation of the input integer. The returned array
	 *         is the smallest array that will hold the value.
	 */
	public static byte[] encodeASCII(final long s) {
		return encodeASCII(Long.toString(s));
	}

	/**
	 * Convert a string to US-ASCII encoding.
	 * 
	 * @param s
	 *            the string to convert. Must not contain any characters over
	 *            127 (outside of 7-bit ASCII).
	 * @return a byte array of the same length as the input string, holding the
	 *         same characters, in the same order.
	 * @throws IllegalArgumentException
	 *             the input string contains one or more characters outside of
	 *             the 7-bit ASCII character space.
	 */
	public static byte[] encodeASCII(final String s) {
		final byte[] r = new byte[s.length()];
		for (int k = r.length - 1; k >= 0; k--) {
			final char c = s.charAt(k);
			if (c > 127)
				throw new IllegalArgumentException("Not ASCII string: " + s);
			r[k] = (byte) c;
		}
		return r;
	}

	static {
		if (OBJECT_ID_LENGTH != newMessageDigest().getDigestLength())
			throw new LinkageError("Incorrect OBJECT_ID_LENGTH.");
		CHARSET = Charset.forName(CHARACTER_ENCODING);
	}

	private Constants() {
		// Hide the default constructor
	}
}
