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
import org.spearce.jgit.revwalk.RevFlag;
import org.spearce.jgit.revwalk.RevFlagSet;
import org.spearce.jgit.revwalk.RevWalk;

/** Matches only commits with some/all RevFlags already set. */
public abstract class RevFlagFilter extends RevFilter {
	/**
	 * Create a new filter that tests for a single flag.
	 * 
	 * @param a
	 *            the flag to test.
	 * @return filter that selects only commits with flag <code>a</code>.
	 */
	public static RevFilter has(final RevFlag a) {
		final RevFlagSet s = new RevFlagSet();
		s.add(a);
		return new HasAll(s);
	}

	/**
	 * Create a new filter that tests all flags in a set.
	 * 
	 * @param a
	 *            set of flags to test.
	 * @return filter that selects only commits with all flags in <code>a</code>.
	 */
	public static RevFilter hasAll(final RevFlag... a) {
		final RevFlagSet set = new RevFlagSet();
		for (final RevFlag flag : a)
			set.add(flag);
		return new HasAll(set);
	}

	/**
	 * Create a new filter that tests all flags in a set.
	 * 
	 * @param a
	 *            set of flags to test.
	 * @return filter that selects only commits with all flags in <code>a</code>.
	 */
	public static RevFilter hasAll(final RevFlagSet a) {
		return new HasAll(new RevFlagSet(a));
	}

	/**
	 * Create a new filter that tests for any flag in a set.
	 * 
	 * @param a
	 *            set of flags to test.
	 * @return filter that selects only commits with any flag in <code>a</code>.
	 */
	public static RevFilter hasAny(final RevFlag... a) {
		final RevFlagSet set = new RevFlagSet();
		for (final RevFlag flag : a)
			set.add(flag);
		return new HasAny(set);
	}

	/**
	 * Create a new filter that tests for any flag in a set.
	 * 
	 * @param a
	 *            set of flags to test.
	 * @return filter that selects only commits with any flag in <code>a</code>.
	 */
	public static RevFilter hasAny(final RevFlagSet a) {
		return new HasAny(new RevFlagSet(a));
	}

	final RevFlagSet flags;

	RevFlagFilter(final RevFlagSet m) {
		flags = m;
	}

	@Override
	public RevFilter clone() {
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + flags;
	}

	private static class HasAll extends RevFlagFilter {
		HasAll(final RevFlagSet m) {
			super(m);
		}

		@Override
		public boolean include(final RevWalk walker, final RevCommit c)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {
			return c.hasAll(flags);
		}
	}

	private static class HasAny extends RevFlagFilter {
		HasAny(final RevFlagSet m) {
			super(m);
		}

		@Override
		public boolean include(final RevWalk walker, final RevCommit c)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {
			return c.hasAny(flags);
		}
	}
}
