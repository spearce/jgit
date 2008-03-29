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

class BoundaryGenerator extends Generator {
	static final int UNINTERESTING = RevWalk.UNINTERESTING;

	Generator g;

	BoundaryGenerator(final RevWalk w, final Generator s) {
		g = new InitialGenerator(w, s);
	}

	@Override
	int outputType() {
		return g.outputType() | HAS_UNINTERESTING;
	}

	@Override
	void shareFreeList(final BlockRevQueue q) {
		g.shareFreeList(q);
	}

	@Override
	RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		return g.next();
	}

	private class InitialGenerator extends Generator {
		private static final int PARSED = RevWalk.PARSED;

		private static final int DUPLICATE = RevWalk.TEMP_MARK;

		private final RevWalk walk;

		private final FIFORevQueue held;

		private final Generator source;

		InitialGenerator(final RevWalk w, final Generator s) {
			walk = w;
			held = new FIFORevQueue();
			source = s;
			source.shareFreeList(held);
		}

		@Override
		int outputType() {
			return source.outputType();
		}

		@Override
		void shareFreeList(final BlockRevQueue q) {
			q.shareFreeList(held);
		}

		@Override
		RevCommit next() throws MissingObjectException,
				IncorrectObjectTypeException, IOException {
			RevCommit c = source.next();
			if (c != null) {
				for (final RevCommit p : c.parents)
					if ((p.flags & UNINTERESTING) != 0)
						held.add(p);
				return c;
			}

			final FIFORevQueue boundary = new FIFORevQueue();
			boundary.shareFreeList(held);
			for (;;) {
				c = held.next();
				if (c == null)
					break;
				if ((c.flags & DUPLICATE) != 0)
					continue;
				if ((c.flags & PARSED) == 0)
					c.parse(walk);
				c.flags |= DUPLICATE;
				boundary.add(c);
			}
			boundary.removeFlag(DUPLICATE);
			g = boundary;
			return boundary.next();
		}
	}
}
