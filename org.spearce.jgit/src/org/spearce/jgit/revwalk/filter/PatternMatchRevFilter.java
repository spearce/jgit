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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.util.RawCharSequence;

/** Abstract filter that searches text using extended regular expressions. */
public abstract class PatternMatchRevFilter extends RevFilter {
	/**
	 * Encode a string pattern for faster matching on byte arrays.
	 * <p>
	 * Force the characters to our funny UTF-8 only convention that we use on
	 * raw buffers. This avoids needing to perform character set decodes on the
	 * individual commit buffers.
	 *
	 * @param patternText
	 *            original pattern string supplied by the user or the
	 *            application.
	 * @return same pattern, but re-encoded to match our funny raw UTF-8
	 *         character sequence {@link RawCharSequence}.
	 */
	protected static final String forceToRaw(final String patternText) {
		final byte[] b;
		try {
			b = patternText.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM lacks UTF-8 support.", e);
		}

		final StringBuilder needle = new StringBuilder(b.length);
		for (int i = 0; i < b.length; i++)
			needle.append((char) (b[i] & 0xff));
		return needle.toString();
	}

	private final String patternText;

	private final Matcher compiledPattern;

	/**
	 * Construct a new pattern matching filter.
	 *
	 * @param pattern
	 *            text of the pattern. Callers may want to surround their
	 *            pattern with ".*" on either end to allow matching in the
	 *            middle of the string.
	 * @param innerString
	 *            should .* be wrapped around the pattern of ^ and $ are
	 *            missing? Most users will want this set.
	 * @param rawEncoding
	 *            should {@link #forceToRaw(String)} be applied to the pattern
	 *            before compiling it?
	 * @param flags
	 *            flags from {@link Pattern} to control how matching performs.
	 */
	protected PatternMatchRevFilter(String pattern, final boolean innerString,
			final boolean rawEncoding, final int flags) {
		if (pattern.length() == 0)
			throw new IllegalArgumentException("Cannot match on empty string.");
		patternText = pattern;

		if (innerString) {
			if (!pattern.startsWith("^") && !pattern.startsWith(".*"))
				pattern = ".*" + pattern;
			if (!pattern.endsWith("$") && !pattern.endsWith(".*"))
				pattern = pattern + ".*";
		}
		final String p = rawEncoding ? forceToRaw(pattern) : pattern;
		compiledPattern = Pattern.compile(p, flags).matcher("");
	}

	/**
	 * Get the pattern this filter uses.
	 *
	 * @return the pattern this filter is applying to candidate strings.
	 */
	public String pattern() {
		return patternText;
	}

	@Override
	public boolean include(final RevWalk walker, final RevCommit cmit)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		return compiledPattern.reset(text(cmit)).matches();
	}

	/**
	 * Obtain the raw text to match against.
	 *
	 * @param cmit
	 *            current commit being evaluated.
	 * @return sequence for the commit's content that we need to match on.
	 */
	protected abstract CharSequence text(RevCommit cmit);

	@Override
	public String toString() {
		return super.toString() + "(\"" + patternText + "\")";
	}
}
