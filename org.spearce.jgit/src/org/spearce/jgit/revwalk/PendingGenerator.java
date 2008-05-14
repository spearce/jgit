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
import org.spearce.jgit.errors.StopWalkException;
import org.spearce.jgit.revwalk.filter.RevFilter;

/**
 * Default (and first pass) RevCommit Generator implementation for RevWalk.
 * <p>
 * This generator starts from a set of one or more commits and process them in
 * descending (newest to oldest) commit time order. Commits automatically cause
 * their parents to be enqueued for further processing, allowing the entire
 * commit graph to be walked. A {@link RevFilter} may be used to select a subset
 * of the commits and return them to the caller.
 */
class PendingGenerator extends Generator {
	private static final int PARSED = RevWalk.PARSED;

	private static final int SEEN = RevWalk.SEEN;

	private static final int UNINTERESTING = RevWalk.UNINTERESTING;

	private final RevWalk walker;

	private final AbstractRevQueue pending;

	private final RevFilter filter;

	private final int output;

	boolean canDispose;

	PendingGenerator(final RevWalk w, final AbstractRevQueue p,
			final RevFilter f, final int out) {
		walker = w;
		pending = p;
		filter = f;
		output = out;
		canDispose = true;
	}

	@Override
	int outputType() {
		if (pending instanceof DateRevQueue)
			return output | SORT_COMMIT_TIME_DESC;
		return output;
	}

	@Override
	RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		try {
			for (;;) {
				final RevCommit c = pending.next();
				if (c == null) {
					walker.curs.release();
					return null;
				}

				final boolean produce;
				if ((c.flags & UNINTERESTING) != 0)
					produce = false;
				else
					produce = filter.include(walker, c);

				for (final RevCommit p : c.parents) {
					if ((p.flags & SEEN) != 0)
						continue;
					if ((p.flags & PARSED) == 0)
						p.parse(walker);
					p.flags |= SEEN;
					pending.add(p);
				}
				walker.carryFlagsImpl(c);

				if ((c.flags & UNINTERESTING) != 0) {
					if (pending.everbodyHasFlag(UNINTERESTING))
						throw StopWalkException.INSTANCE;
					c.dispose();
					continue;
				}

				if (produce)
					return c;
				else if (canDispose)
					c.dispose();
			}
		} catch (StopWalkException swe) {
			walker.curs.release();
			pending.clear();
			return null;
		}
	}
}
