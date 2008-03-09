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

import java.io.UnsupportedEncodingException;

/**
 * Searches text using only substring search.
 * <p>
 * Instances are thread-safe. Multiple concurrent threads may perform matches on
 * different character sequences at the same time.
 */
public class RawSubStringPattern {
	private final String needleString;

	private final byte[] needle;

	/**
	 * Construct a new substring pattern.
	 * 
	 * @param patternText
	 *            text to locate. This should be a literal string, as no
	 *            meta-characters are supported by this implementation. The
	 *            string may not be the empty string.
	 */
	public RawSubStringPattern(final String patternText) {
		if (patternText.length() == 0)
			throw new IllegalArgumentException("Cannot match on empty string.");
		needleString = patternText;

		final byte[] b;
		try {
			b = patternText.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM lacks UTF-8 support.", e);
		}

		needle = new byte[b.length];
		for (int i = 0; i < b.length; i++)
			needle[i] = lc(b[i]);
	}

	/**
	 * Match a character sequence against this pattern.
	 * 
	 * @param rcs
	 *            the sequence to match. Must not be null but the length of the
	 *            sequence is permitted to be 0.
	 * @return offset within <code>rcs</code> of the first occurrence of this
	 *         pattern; -1 if this pattern does not appear at any position of
	 *         <code>rcs</code>.
	 */
	public int match(final RawCharSequence rcs) {
		final int needleLen = needle.length;
		final byte first = needle[0];

		final byte[] text = rcs.buffer;
		int matchPos = rcs.startPtr;
		final int maxPos = rcs.endPtr - needleLen;

		OUTER: for (; matchPos < maxPos; matchPos++) {
			if (neq(first, text[matchPos])) {
				while (++matchPos < maxPos && neq(first, text[matchPos])) {
					/* skip */
				}
				if (matchPos == maxPos)
					return -1;
			}

			int si = ++matchPos;
			for (int j = 1; j < needleLen; j++, si++) {
				if (neq(needle[j], text[si]))
					continue OUTER;
			}
			return matchPos - 1;
		}
		return -1;
	}

	private static final boolean neq(final byte a, final byte b) {
		return a != b && a != lc(b);
	}

	private static final byte lc(final byte q) {
		return (byte) Character.toLowerCase((char) (q & 0xff));
	}

	/**
	 * Get the literal pattern string this instance searches for.
	 * 
	 * @return the pattern string given to our constructor.
	 */
	public String pattern() {
		return needleString;
	}

	@Override
	public String toString() {
		return pattern();
	}
}
