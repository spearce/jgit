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

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.revwalk.filter.AndRevFilter;
import org.spearce.jgit.revwalk.filter.NotRevFilter;
import org.spearce.jgit.revwalk.filter.OrRevFilter;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Selects interesting tree entries during walking.
 * <p>
 * This is an abstract interface. Applications may implement a subclass, or use
 * one of the predefined implementations already available within this package.
 * <p>
 * Unless specifically noted otherwise a TreeFilter implementation is not thread
 * safe and may not be shared by different TreeWalk instances at the same time.
 * This restriction allows TreeFilter implementations to cache state within
 * their instances during {@link #include(TreeWalk)} if it is beneficial to
 * their implementation. Deep clones created by {@link #clone()} may be used to
 * construct a thread-safe copy of an existing filter.
 * 
 * <p>
 * <b>Difference filters:</b>
 * <ul>
 * <li>Only select differences: {@link #ANY_DIFF}.</li>
 * </ul>
 * 
 * <p>
 * <b>Boolean modifiers:</b>
 * <ul>
 * <li>AND: {@link AndRevFilter}</li>
 * <li>OR: {@link OrRevFilter}</li>
 * <li>NOT: {@link NotRevFilter}</li>
 * </ul>
 */
public abstract class TreeFilter {
	/** Selects all tree entries. */
	public static final TreeFilter ALL = new TreeFilter() {
		@Override
		public boolean include(final TreeWalk walker) {
			return true;
		}

		@Override
		public TreeFilter clone() {
			return this;
		}

		@Override
		public String toString() {
			return "ALL";
		}
	};

	/**
	 * Selects only tree entries which differ between at least 2 trees.
	 * <p>
	 * This filter also prevents a TreeWalk from recursing into a subtree if all
	 * parent trees have the identical subtree at the same path. This
	 * dramatically improves walk performance as only the changed subtrees are
	 * entered into.
	 * <p>
	 * If this filter is applied to a walker with only one tree it behaves like
	 * {@link #ALL}, or as though the walker was matching a virtual empty tree
	 * against the single tree it was actually given. Applications may wish to
	 * treat such a difference as "all names added".
	 */
	public static final TreeFilter ANY_DIFF = new TreeFilter() {
		private static final int baseTree = 0;

		@Override
		public boolean include(final TreeWalk walker) {
			final int n = walker.getTreeCount();
			if (n == 1) // Assume they meant difference to empty tree.
				return true;

			final int m = walker.getRawMode(baseTree);
			for (int i = 1; i < n; i++)
				if (walker.getRawMode(i) != m || !walker.idEqual(i, baseTree))
					return true;
			return false;
		}

		@Override
		public TreeFilter clone() {
			return this;
		}

		@Override
		public String toString() {
			return "ANY_DIFF";
		}
	};

	/**
	 * Create a new filter that does the opposite of this filter.
	 * 
	 * @return a new filter that includes tree entries this filter rejects.
	 */
	public TreeFilter negate() {
		return NotTreeFilter.create(this);
	}

	/**
	 * Determine if the current entry is interesting to report.
	 * <p>
	 * This method is consulted for subtree entries even if
	 * {@link TreeWalk#isRecursive()} is enabled. The consultation allows the
	 * filter to bypass subtree recursion on a case-by-case basis, even when
	 * recursion is enabled at the application level.
	 * 
	 * @param walker
	 *            the walker the filter needs to examine.
	 * @return true if the current entry should be seen by the application;
	 *         false to hide the entry.
	 * @throws MissingObjectException
	 *             an object the filter needs to consult to determine its answer
	 *             does not exist in the Git repository the walker is operating
	 *             on. Filtering this current walker entry is impossible without
	 *             the object.
	 * @throws IncorrectObjectTypeException
	 *             an object the filter needed to consult was not of the
	 *             expected object type. This usually indicates a corrupt
	 *             repository, as an object link is referencing the wrong type.
	 * @throws IOException
	 *             a loose object or pack file could not be read to obtain data
	 *             necessary for the filter to make its decision.
	 */
	public abstract boolean include(TreeWalk walker)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException;

	/**
	 * Clone this tree filter, including its parameters.
	 * <p>
	 * This is a deep clone. If this filter embeds objects or other filters it
	 * must also clone those, to ensure the instances do not share mutable data.
	 * 
	 * @return another copy of this filter, suitable for another thread.
	 */
	public abstract TreeFilter clone();

	@Override
	public String toString() {
		String n = getClass().getName();
		int lastDot = n.lastIndexOf('.');
		if (lastDot >= 0) {
			n = n.substring(lastDot + 1);
		}
		return n.replace('$', '.');
	}
}
