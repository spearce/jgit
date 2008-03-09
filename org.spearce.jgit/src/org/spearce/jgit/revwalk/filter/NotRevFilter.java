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

/** Includes a commit only if the subfilter does not include the commit. */
public class NotRevFilter extends RevFilter {
	/**
	 * Create a filter that negates the result of another filter.
	 *
	 * @param a
	 *            filter to negate.
	 * @return a filter that does the reverse of <code>a</code>.
	 */
	public static RevFilter create(final RevFilter a) {
		return new NotRevFilter(a);
	}

	private final RevFilter a;

	private NotRevFilter(final RevFilter one) {
		a = one;
	}

	@Override
	public RevFilter negate() {
		return a;
	}

	@Override
	public boolean include(final RevWalk walker, final RevCommit c)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		return !a.include(walker, c);
	}

	@Override
	public RevFilter clone() {
		return new NotRevFilter(a.clone());
	}

	@Override
	public String toString() {
		return "NOT " + a.toString();
	}
}
