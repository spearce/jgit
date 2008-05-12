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
 * Computes the merge base(s) of the starting commits.
 * <p>
 * This generator is selected if the RevFilter is only
 * {@link org.spearce.jgit.revwalk.filter.RevFilter#MERGE_BASE}.
 * <p>
 * To compute the merge base we assign a temporary flag to each of the starting
 * commits. The maximum number of starting commits is bounded by the number of
 * free flags available in the RevWalk when the generator is initialized. These
 * flags will be automatically released on the next reset of the RevWalk, but
 * not until then, as they are assigned to commits throughout the history.
 * <p>
 * Several internal flags are reused here for a different purpose, but this
 * should not have any impact as this generator should be run alone, and without
 * any other generators wrapped around it.
 */
class MergeBaseGenerator extends Generator {
	private static final int PARSED = RevWalk.PARSED;

	private static final int IN_PENDING = RevWalk.SEEN;

	private static final int POPPED = RevWalk.TEMP_MARK;

	private static final int MERGE_BASE = RevWalk.REWRITE;

	private final RevWalk walker;

	private final DateRevQueue pending;

	private int branchMask;

	MergeBaseGenerator(final RevWalk w) {
		walker = w;
		pending = new DateRevQueue();
	}

	void init(final AbstractRevQueue p) {
		try {
			for (;;) {
				final RevCommit c = p.next();
				if (c == null)
					break;
				add(c);
			}
		} finally {
			// Always free the flags immediately. This ensures the flags
			// will be available for reuse when the walk resets.
			//
			walker.freeFlag(branchMask);
		}
	}

	private void add(final RevCommit c) {
		final int flag = walker.allocFlag();
		branchMask |= flag;
		if ((c.flags & branchMask) != 0) {
			// This should never happen. RevWalk ensures we get a
			// commit admitted to the initial queue only once. If
			// we see this marks aren't correctly erased.
			//
			throw new IllegalStateException("Stale RevFlags found on " + c);
		}
		c.flags |= flag;
		pending.add(c);
	}

	@Override
	int outputType() {
		return 0;
	}

	@Override
	RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		for (;;) {
			final RevCommit c = pending.next();
			if (c == null) {
				walker.curs.release();
				return null;
			}

			for (final RevCommit p : c.parents) {
				if ((p.flags & IN_PENDING) != 0)
					continue;
				if ((p.flags & PARSED) == 0)
					p.parse(walker);
				p.flags |= IN_PENDING;
				pending.add(p);
			}

			int carry = c.flags & branchMask;
			boolean mb = carry == branchMask;
			if (mb) {
				// If we are a merge base make sure our ancestors are
				// also flagged as being popped, so that they do not
				// generate to the caller.
				//
				carry |= MERGE_BASE;
			}
			carryOntoHistory(c, carry);

			if ((c.flags & MERGE_BASE) != 0) {
				// This commit is an ancestor of a merge base we already
				// popped back to the caller. If everyone in pending is
				// that way we are done traversing; if not we just need
				// to move to the next available commit and try again.
				//
				if (pending.everbodyHasFlag(MERGE_BASE))
					return null;
				continue;
			}
			c.flags |= POPPED;

			if (mb) {
				c.flags |= MERGE_BASE;
				return c;
			}
		}
	}

	private void carryOntoHistory(RevCommit c, final int carry) {
		for (;;) {
			final RevCommit[] pList = c.parents;
			if (pList == null)
				return;
			final int n = pList.length;
			if (n == 0)
				return;

			for (int i = 1; i < n; i++) {
				final RevCommit p = pList[i];
				if (!carryOntoOne(p, carry))
					carryOntoHistory(p, carry);
			}

			c = pList[0];
			if (carryOntoOne(c, carry))
				break;
		}
	}

	private boolean carryOntoOne(final RevCommit p, final int carry) {
		p.flags |= carry;
		if ((p.flags & POPPED) != 0 && (carry & MERGE_BASE) == 0
				&& (p.flags & branchMask) == branchMask) {
			// We were popped without being a merge base, but we just got
			// voted to be one. Inject ourselves back at the front of the
			// pending queue and tell all of our ancestors they are within
			// the merge base now.
			//
			p.flags &= ~POPPED;
			pending.add(p);
			carryOntoHistory(p, branchMask | MERGE_BASE);
			return true;
		}
		return false;
	}
}
