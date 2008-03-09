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

import java.util.regex.Pattern;

import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.util.RawCharSequence;

/** Matches only commits whose message matches the pattern. */
public class MessageRevFilter {
	/**
	 * Create a message filter.
	 * <p>
	 * An optimized substring search may be automatically selected if the
	 * pattern does not contain any regular expression meta-characters.
	 * <p>
	 * The search is performed using a case-insensitive comparison. The
	 * character encoding of the commit message itself is not respected. The
	 * filter matches on raw UTF-8 byte sequences.
	 *
	 * @param pattern
	 *            regular expression pattern to match.
	 * @return a new filter that matches the given expression against the
	 *         message body of the commit.
	 */
	public static RevFilter create(String pattern) {
		if (pattern.length() == 0)
			throw new IllegalArgumentException("Cannot match on empty string.");
		if (SubStringRevFilter.safe(pattern))
			return new SubStringSearch(pattern);
		return new PatternSearch(pattern);
	}

	private MessageRevFilter() {
		// Don't permit us to be created.
	}

	static RawCharSequence textFor(final RevCommit cmit) {
		final byte[] raw = cmit.getRawBuffer();
		final int b = RawParseUtils.commitMessage(raw, 0);
		if (b < 0)
			return RawCharSequence.EMPTY;
		return new RawCharSequence(raw, b, raw.length);
	}

	private static class PatternSearch extends PatternMatchRevFilter {
		PatternSearch(final String patternText) {
			super(patternText, true, true, Pattern.CASE_INSENSITIVE
					| Pattern.DOTALL);
		}

		@Override
		protected CharSequence text(final RevCommit cmit) {
			return textFor(cmit);
		}

		@Override
		public RevFilter clone() {
			return new PatternSearch(pattern());
		}
	}

	private static class SubStringSearch extends SubStringRevFilter {
		SubStringSearch(final String patternText) {
			super(patternText);
		}

		@Override
		protected RawCharSequence text(final RevCommit cmit) {
			return textFor(cmit);
		}
	}
}
