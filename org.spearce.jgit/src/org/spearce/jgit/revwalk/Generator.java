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
