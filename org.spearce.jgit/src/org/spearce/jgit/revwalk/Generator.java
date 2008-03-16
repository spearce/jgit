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

import java.io.IOException;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;

/**
 * Produces commits for RevWalk to return to applications.
 * <p>
 * Implementations of this basic class provide the real work behind RevWalk.
 * Conceptually a Generator is an iterator or a queue, it returns commits until
 * there are no more relevant. Generators may be piped/stacked together to
 * create a more complex set of operations.
 * 
 * @see AbstractPendingGenerator
 * @see StartGenerator
 */
abstract class Generator {
	/** Commits are sorted by commit date and time, descending. */
	static final int SORT_COMMIT_TIME_DESC = 1 << 0;

	/** Output may have {@link RevWalk#REWRITE} marked on it. */
	static final int HAS_REWRITE = 1 << 1;

	/** Output needs {@link RewriteGenerator}. */
	static final int NEEDS_REWRITE = 1 << 2;

	/** Topological ordering is enforced (all children before parents). */
	static final int SORT_TOPO = 1 << 3;

	/**
	 * Register a commit as a starting point.
	 * <p>
	 * The default implementation of this method always throws an exception as
	 * most generators do not accept commits directly. Instead they pull them
	 * from another generator, or they are passed the set of relevant commits
	 * when created.
	 * 
	 * @param c
	 *            the commit the application wants to start from.
	 */
	void add(final RevCommit c) {
		throw new IllegalStateException("Revision walk has already begun;"
				+ " it is too late to add commit " + c.getId() + ".");
	}

	/**
	 * Connect the supplied queue to this generator's own free list (if any).
	 * 
	 * @param q
	 *            another FIFO queue that wants to share our queue's free list.
	 */
	void shareFreeList(final BlockRevQueue q) {
		// Do nothing by default.
	}

	/**
	 * Obtain flags describing the output behavior of this generator.
	 * 
	 * @return one or more of the constants declared in this class, describing
	 *         how this generator produces its results.
	 */
	abstract int outputType();

	/**
	 * Return the next commit to the application, or the next generator.
	 * 
	 * @return next available commit; null if no more are to be returned.
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	abstract RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException;
}
