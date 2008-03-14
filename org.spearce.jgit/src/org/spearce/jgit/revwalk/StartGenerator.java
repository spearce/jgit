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
import org.spearce.jgit.revwalk.filter.RevFilter;
import org.spearce.jgit.treewalk.filter.TreeFilter;

/**
 * Initial RevWalk generator that bootstraps a new walk.
 * <p>
 * Initially RevWalk starts with this generator as its chosen implementation.
 * The first request for a RevCommit from the RevWalk instance calls to our
 * {@link #next()} method, and we replace ourselves with the best Generator
 * implementation available based upon the current RevWalk configuration.
 */
class StartGenerator extends Generator {
	private final RevWalk walker;

	private final DateRevQueue pending;

	StartGenerator(final RevWalk w) {
		walker = w;
		pending = new DateRevQueue();
	}

	@Override
	void add(final RevCommit c) {
		pending.add(c);
	}

	@Override
	RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		Generator g;

		final RevWalk w = walker;
		final RevFilter rf = w.getRevFilter();
		final TreeFilter tf = w.getTreeFilter();

		if (tf != TreeFilter.ALL) {
			g = new TreeFilterPendingGenerator(w, pending, rf, tf);
			g = new BufferGenerator(g);
			g = new RewriteGenerator(g);
		} else {
			g = new AbstractPendingGenerator(w, pending, rf) {
				@Override
				boolean include(final RevCommit c) {
					return true;
				}
			};
		}

		w.pending = g;
		return g.next();
	}
}
