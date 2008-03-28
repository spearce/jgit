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

/** Sorts commits in topological order. */
class TopoSortGenerator extends Generator {
	private static final int TOPO_DELAY = RevWalk.TOPO_DELAY;

	private final FIFORevQueue pending;

	private final int outputType;

	/**
	 * Create a new sorter and completely spin the generator.
	 * <p>
	 * When the constructor completes the supplied generator will have no
	 * commits remaining, as all of the commits will be held inside of this
	 * generator's internal buffer.
	 * 
	 * @param s
	 *            generator to pull all commits out of, and into this buffer.
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	TopoSortGenerator(final Generator s) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		pending = new FIFORevQueue();
		outputType = s.outputType() | SORT_TOPO;
		s.shareFreeList(pending);
		for (;;) {
			final RevCommit c = s.next();
			if (c == null)
				break;
			for (final RevCommit p : c.parents)
				p.inDegree++;
			pending.add(c);
		}
	}

	@Override
	int outputType() {
		return outputType;
	}

	@Override
	void shareFreeList(final BlockRevQueue q) {
		q.shareFreeList(pending);
	}

	@Override
	RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		for (;;) {
			final RevCommit c = pending.next();
			if (c == null)
				return null;

			if (c.inDegree > 0) {
				// At least one of our children is missing. We delay
				// production until all of our children are output.
				//
				c.flags |= TOPO_DELAY;
				continue;
			}

			// All of our children have already produced,
			// so it is OK for us to produce now as well.
			//
			for (final RevCommit p : c.parents) {
				if (--p.inDegree == 0 && (p.flags & TOPO_DELAY) != 0) {
					// This parent tried to come before us, but we are
					// his last child. unpop the parent so it goes right
					// behind this child.
					//
					p.flags &= ~TOPO_DELAY;
					pending.unpop(p);
				}
			}
			return c;
		}
	}
}
