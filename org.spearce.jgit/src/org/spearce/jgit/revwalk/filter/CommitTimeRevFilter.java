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
package org.spearce.jgit.revwalk.filter;

import java.io.IOException;
import java.util.Date;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.errors.StopWalkException;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;

/** Selects commits based upon the commit time field. */
public abstract class CommitTimeRevFilter extends RevFilter {
	/**
	 * Create a new filter to select commits before a given date/time.
	 * 
	 * @param ts
	 *            the point in time to cut on.
	 * @return a new filter to select commits on or before <code>ts</code>.
	 */
	public static final RevFilter before(final Date ts) {
		return new Before(ts.getTime());
	}

	/**
	 * Create a new filter to select commits after a given date/time.
	 * 
	 * @param ts
	 *            the point in time to cut on.
	 * @return a new filter to select commits on or after <code>ts</code>.
	 */
	public static final RevFilter after(final Date ts) {
		return new After(ts.getTime());
	}

	final int when;

	CommitTimeRevFilter(final long ts) {
		when = (int) (ts / 1000);
	}

	@Override
	public RevFilter clone() {
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + new Date(when * 1000) + ")";
	}

	private static class Before extends CommitTimeRevFilter {
		Before(final long ts) {
			super(ts);
		}

		@Override
		public boolean include(final RevWalk walker, final RevCommit cmit)
				throws StopWalkException, MissingObjectException,
				IncorrectObjectTypeException, IOException {
			return cmit.getCommitTime() <= when;
		}
	}

	private static class After extends CommitTimeRevFilter {
		After(final long ts) {
			super(ts);
		}

		@Override
		public boolean include(final RevWalk walker, final RevCommit cmit)
				throws StopWalkException, MissingObjectException,
				IncorrectObjectTypeException, IOException {
			// Since the walker sorts commits by commit time we can be
			// reasonably certain there is nothing remaining worth our
			// scanning if this commit is before the point in question.
			//
			if (cmit.getCommitTime() < when)
				throw StopWalkException.INSTANCE;
			return true;
		}
	}
}
