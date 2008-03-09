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
package org.spearce.jgit.revwalk.filter;

import org.spearce.jgit.lib.Constants;

/** Handy utility functions to parse raw object contents. */
public final class RawParseUtils {
	private static final byte[] author = Constants.encodeASCII("author ");

	private static final byte[] committer = Constants.encodeASCII("committer ");

	private static final int match(final byte[] b, int ptr, final byte[] src) {
		if (ptr + src.length >= b.length)
			return -1;
		for (int i = 0; i < src.length; i++, ptr++)
			if (b[ptr] != src[i])
				return -1;
		return ptr;
	}

	/**
	 * Locate the first position after a given character.
	 *
	 * @param b
	 *            buffer to scan.
	 * @param ptr
	 *            position within buffer to start looking for LF at.
	 * @param chrA
	 *            character to find.
	 * @return new position just after chr.
	 */
	public static final int next(final byte[] b, int ptr, final char chrA) {
		final int sz = b.length;
		while (ptr < sz) {
			if (b[ptr] == chrA)
				return ptr + 1;
			else
				ptr++;
		}
		return ptr;
	}

	/**
	 * Locate the first position after either the given character or LF.
	 * <p>
	 * This method stops on the first match it finds from either chrA or '\n'.
	 *
	 * @param b
	 *            buffer to scan.
	 * @param ptr
	 *            position within buffer to start looking for LF at.
	 * @param chrA
	 *            character to find.
	 * @return new position just after the first chrA or chrB to be found.
	 */
	public static final int nextLF(final byte[] b, int ptr, final char chrA) {
		final int sz = b.length;
		while (ptr < sz) {
			final byte c = b[ptr];
			if (c == chrA || c == '\n')
				return ptr + 1;
			else
				ptr++;
		}
		return ptr;
	}

	/**
	 * Locate the "author " header line data.
	 *
	 * @param b
	 *            buffer to scan.
	 * @param ptr
	 *            position in buffer to start the scan at. Most callers should
	 *            pass 0 to ensure the scan starts from the beginning of the
	 *            commit buffer and does not accidentally look at message body.
	 * @return position just after the space in "author ", so the first
	 *         character of the author's name. If no author header can be
	 *         located -1 is returned.
	 */
	public static final int author(final byte[] b, int ptr) {
		final int sz = b.length;
		if (ptr == 0)
			ptr += 46; // skip the "tree ..." line.
		while (ptr < sz && b[ptr] == 'p')
			ptr += 48; // skip this parent.
		return match(b, ptr, author);
	}

	/**
	 * Locate the "committer " header line data.
	 *
	 * @param b
	 *            buffer to scan.
	 * @param ptr
	 *            position in buffer to start the scan at. Most callers should
	 *            pass 0 to ensure the scan starts from the beginning of the
	 *            commit buffer and does not accidentally look at message body.
	 * @return position just after the space in "committer ", so the first
	 *         character of the committer's name. If no committer header can be
	 *         located -1 is returned.
	 */
	public static final int committer(final byte[] b, int ptr) {
		final int sz = b.length;
		if (ptr == 0)
			ptr += 46; // skip the "tree ..." line.
		while (ptr < sz && b[ptr] == 'p')
			ptr += 48; // skip this parent.
		if (ptr < sz && b[ptr] == 'a')
			ptr = next(b, ptr, '\n');
		return match(b, ptr, committer);
	}

	/**
	 * Locate the position of the commit message body.
	 *
	 * @param b
	 *            buffer to scan.
	 * @param ptr
	 *            position in buffer to start the scan at. Most callers should
	 *            pass 0 to ensure the scan starts from the beginning of the
	 *            commit buffer.
	 * @return position of the user's message buffer.
	 */
	public static final int commitMessage(final byte[] b, int ptr) {
		final int sz = b.length;
		if (ptr == 0)
			ptr += 46; // skip the "tree ..." line.
		while (ptr < sz && b[ptr] == 'p')
			ptr += 48; // skip this parent.

		// skip any remaining header lines, ignoring what their actual
		// header line type is.
		//
		while (ptr < sz && b[ptr] != '\n')
			ptr = next(b, ptr, '\n');
		if (ptr < sz && b[ptr] == '\n')
			return ptr + 1;
		return -1;
	}

	private RawParseUtils() {
		// Don't create instances of a static only utility.
	}
}
