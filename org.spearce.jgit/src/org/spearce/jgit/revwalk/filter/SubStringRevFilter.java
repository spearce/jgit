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

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.util.RawCharSequence;
import org.spearce.jgit.util.RawSubStringPattern;

/** Abstract filter that searches text using only substring search. */
public abstract class SubStringRevFilter extends RevFilter {
	/**
	 * Can this string be safely handled by a substring filter?
	 * 
	 * @param pattern
	 *            the pattern text proposed by the user.
	 * @return true if a substring filter can perform this pattern match; false
	 *         if {@link PatternMatchRevFilter} must be used instead.
	 */
	public static boolean safe(final String pattern) {
		for (int i = 0; i < pattern.length(); i++) {
			final char c = pattern.charAt(i);
			switch (c) {
			case '.':
			case '?':
			case '*':
			case '+':
			case '{':
			case '}':
			case '(':
			case ')':
			case '[':
			case ']':
			case '\\':
				return false;
			}
		}
		return true;
	}

	private final RawSubStringPattern pattern;

	/**
	 * Construct a new matching filter.
	 * 
	 * @param patternText
	 *            text to locate. This should be a safe string as described by
	 *            the {@link #safe(String)} as regular expression meta
	 *            characters are treated as literals.
	 */
	protected SubStringRevFilter(final String patternText) {
		pattern = new RawSubStringPattern(patternText);
	}

	@Override
	public boolean include(final RevWalk walker, final RevCommit cmit)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		return pattern.match(text(cmit)) >= 0;
	}

	/**
	 * Obtain the raw text to match against.
	 * 
	 * @param cmit
	 *            current commit being evaluated.
	 * @return sequence for the commit's content that we need to match on.
	 */
	protected abstract RawCharSequence text(RevCommit cmit);

	@Override
	public String toString() {
		return super.toString() + "(\"" + pattern.pattern() + "\")";
	}
}
