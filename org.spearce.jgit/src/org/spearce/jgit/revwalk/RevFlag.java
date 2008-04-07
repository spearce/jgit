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
package org.spearce.jgit.revwalk;

/**
 * Application level mark bit for {@link RevObject}s.
 * <p>
 * To create a flag use {@link RevWalk#newFlag(String)}.
 */
public class RevFlag {
	/**
	 * Uninteresting by {@link RevWalk#markUninteresting(RevCommit)}.
	 * <p>
	 * We flag commits as uninteresting if the caller does not want commits
	 * reachable from a commit to {@link RevWalk#markUninteresting(RevCommit)}.
	 * This flag is always carried into the commit's parents and is a key part
	 * of the "rev-list B --not A" feature; A is marked UNINTERESTING.
	 * <p>
	 * This is a static flag. Its RevWalk is not available.
	 */
	public static final RevFlag UNINTERESTING = new StaticRevFlag(
			"UNINTERESTING", RevWalk.UNINTERESTING);

	final RevWalk walker;

	final String name;

	final int mask;

	RevFlag(final RevWalk w, final String n, final int m) {
		walker = w;
		name = n;
		mask = m;
	}

	/**
	 * Get the revision walk instance this flag was created from.
	 * 
	 * @return the walker this flag was allocated out of, and belongs to.
	 */
	public RevWalk getRevWalk() {
		return walker;
	}

	public String toString() {
		return name;
	}

	static class StaticRevFlag extends RevFlag {
		StaticRevFlag(final String n, final int m) {
			super(null, n, m);
		}

		@Override
		public RevWalk getRevWalk() {
			throw new UnsupportedOperationException(toString()
					+ " is a static flag and has no RevWalk instance");
		}
	}
}
