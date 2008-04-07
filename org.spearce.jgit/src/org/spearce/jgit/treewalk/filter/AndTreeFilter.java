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
package org.spearce.jgit.treewalk.filter;

import java.io.IOException;
import java.util.Collection;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Includes a tree entry only if all subfilters include the same tree entry.
 * <p>
 * Classic shortcut behavior is used, so evaluation of the
 * {@link TreeFilter#include(TreeWalk)} method stops as soon as a false result
 * is obtained. Applications can improve filtering performance by placing faster
 * filters that are more likely to reject a result earlier in the list.
 */
public abstract class AndTreeFilter extends TreeFilter {
	/**
	 * Create a filter with two filters, both of which must match.
	 * 
	 * @param a
	 *            first filter to test.
	 * @param b
	 *            second filter to test.
	 * @return a filter that must match both input filters.
	 */
	public static TreeFilter create(final TreeFilter a, final TreeFilter b) {
		return new Binary(a, b);
	}

	/**
	 * Create a filter around many filters, all of which must match.
	 * 
	 * @param list
	 *            list of filters to match against. Must contain at least 2
	 *            filters.
	 * @return a filter that must match all input filters.
	 */
	public static TreeFilter create(final TreeFilter[] list) {
		if (list.length == 2)
			return create(list[0], list[1]);
		if (list.length < 2)
			throw new IllegalArgumentException("At least two filters needed.");
		final TreeFilter[] subfilters = new TreeFilter[list.length];
		System.arraycopy(list, 0, subfilters, 0, list.length);
		return new List(subfilters);
	}

	/**
	 * Create a filter around many filters, all of which must match.
	 * 
	 * @param list
	 *            list of filters to match against. Must contain at least 2
	 *            filters.
	 * @return a filter that must match all input filters.
	 */
	public static TreeFilter create(final Collection<TreeFilter> list) {
		if (list.size() < 2)
			throw new IllegalArgumentException("At least two filters needed.");
		final TreeFilter[] subfilters = new TreeFilter[list.size()];
		list.toArray(subfilters);
		if (subfilters.length == 2)
			return create(subfilters[0], subfilters[1]);
		return new List(subfilters);
	}

	private static class Binary extends AndTreeFilter {
		private final TreeFilter a;

		private final TreeFilter b;

		Binary(final TreeFilter one, final TreeFilter two) {
			a = one;
			b = two;
		}

		@Override
		public boolean include(final TreeWalk walker)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {
			return a.include(walker) && b.include(walker);
		}

		@Override
		public boolean shouldBeRecursive() {
			return a.shouldBeRecursive() || b.shouldBeRecursive();
		}

		@Override
		public TreeFilter clone() {
			return new Binary(a.clone(), b.clone());
		}

		@Override
		public String toString() {
			return "(" + a.toString() + " AND " + b.toString() + ")";
		}
	}

	private static class List extends AndTreeFilter {
		private final TreeFilter[] subfilters;

		List(final TreeFilter[] list) {
			subfilters = list;
		}

		@Override
		public boolean include(final TreeWalk walker)
				throws MissingObjectException, IncorrectObjectTypeException,
				IOException {
			for (final TreeFilter f : subfilters) {
				if (!f.include(walker))
					return false;
			}
			return true;
		}

		@Override
		public boolean shouldBeRecursive() {
			for (final TreeFilter f : subfilters)
				if (f.shouldBeRecursive())
					return true;
			return false;
		}

		@Override
		public TreeFilter clone() {
			final TreeFilter[] s = new TreeFilter[subfilters.length];
			for (int i = 0; i < s.length; i++)
				s[i] = subfilters[i].clone();
			return new List(s);
		}

		@Override
		public String toString() {
			final StringBuffer r = new StringBuffer();
			r.append("(");
			for (int i = 0; i < subfilters.length; i++) {
				if (i > 0)
					r.append(" AND ");
				r.append(subfilters[i].toString());
			}
			r.append(")");
			return r.toString();
		}
	}
}
