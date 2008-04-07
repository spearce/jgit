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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.spearce.jgit.errors.StopWalkException;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Includes tree entries only if they match one or more configured paths.
 * <p>
 * Operates like {@link PathFilter} but causes the walk to abort as soon as the
 * tree can no longer match any of the paths within the group. This may bypass
 * the boolean logic of a higher level AND or OR group, but does improve
 * performance for the common case of examining one or more modified paths.
 * <p>
 * This filter is effectively an OR group around paths, with the early abort
 * feature described above.
 */
public class PathFilterGroup {
	/**
	 * Create a collection of path filters from Java strings.
	 * <p>
	 * Path strings are relative to the root of the repository. If the user's
	 * input should be assumed relative to a subdirectory of the repository the
	 * caller must prepend the subdirectory's path prior to creating the filter.
	 * <p>
	 * Path strings use '/' to delimit directories on all platforms.
	 * <p>
	 * Paths may appear in any order within the collection. Sorting may be done
	 * internally when the group is constructed if doing so will improve path
	 * matching performance.
	 * 
	 * @param paths
	 *            the paths to test against. Must have at least one entry.
	 * @return a new filter for the list of paths supplied.
	 */
	public static TreeFilter createFromStrings(final Collection<String> paths) {
		if (paths.isEmpty())
			throw new IllegalArgumentException("At least one path is required.");
		final PathFilter[] p = new PathFilter[paths.size()];
		int i = 0;
		for (final String s : paths)
			p[i++] = PathFilter.create(s);
		return create(p);
	}

	/**
	 * Create a collection of path filters.
	 * <p>
	 * Paths may appear in any order within the collection. Sorting may be done
	 * internally when the group is constructed if doing so will improve path
	 * matching performance.
	 * 
	 * @param paths
	 *            the paths to test against. Must have at least one entry.
	 * @return a new filter for the list of paths supplied.
	 */
	public static TreeFilter create(final Collection<PathFilter> paths) {
		if (paths.isEmpty())
			throw new IllegalArgumentException("At least one path is required.");
		final PathFilter[] p = new PathFilter[paths.size()];
		paths.toArray(p);
		return create(p);
	}

	private static TreeFilter create(final PathFilter[] p) {
		if (p.length == 1)
			return new Single(p[0]);
		return new Group(p);
	}

	static class Single extends TreeFilter {
		private final PathFilter path;

		private final byte[] raw;

		private Single(final PathFilter p) {
			path = p;
			raw = path.pathRaw;
		}

		@Override
		public boolean include(final TreeWalk walker) {
			final int cmp = walker.isPathPrefix(raw, raw.length);
			if (cmp > 0)
				throw StopWalkException.INSTANCE;
			return cmp == 0;
		}

		@Override
		public boolean shouldBeRecursive() {
			return path.shouldBeRecursive();
		}

		@Override
		public TreeFilter clone() {
			return this;
		}

		public String toString() {
			return "FAST_" + path.toString();
		}
	}

	static class Group extends TreeFilter {
		private static final Comparator<PathFilter> PATH_SORT = new Comparator<PathFilter>() {
			public int compare(final PathFilter o1, final PathFilter o2) {
				return o1.pathStr.compareTo(o2.pathStr);
			}
		};

		private final PathFilter[] paths;

		private Group(final PathFilter[] p) {
			paths = p;
			Arrays.sort(paths, PATH_SORT);
		}

		@Override
		public boolean include(final TreeWalk walker) {
			final int n = paths.length;
			for (int i = 0;;) {
				final byte[] r = paths[i].pathRaw;
				final int cmp = walker.isPathPrefix(r, r.length);
				if (cmp == 0)
					return true;
				if (++i < n)
					continue;
				if (cmp > 0)
					throw StopWalkException.INSTANCE;
				return false;
			}
		}

		@Override
		public boolean shouldBeRecursive() {
			for (final PathFilter p : paths)
				if (p.shouldBeRecursive())
					return true;
			return false;
		}

		@Override
		public TreeFilter clone() {
			return this;
		}

		public String toString() {
			final StringBuffer r = new StringBuffer();
			r.append("FAST(");
			for (int i = 0; i < paths.length; i++) {
				if (i > 0)
					r.append(" OR ");
				r.append(paths[i].toString());
			}
			r.append(")");
			return r.toString();
		}
	}
}
