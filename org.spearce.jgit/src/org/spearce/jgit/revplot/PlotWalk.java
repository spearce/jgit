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
package org.spearce.jgit.revplot;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevSort;
import org.spearce.jgit.revwalk.RevWalk;

/** Specialized RevWalk for visualization of a commit graph. */
public class PlotWalk extends RevWalk {
	/**
	 * Create a new revision walker for a given repository.
	 * 
	 * @param repo
	 *            the repository the walker will obtain data from.
	 */
	public PlotWalk(final Repository repo) {
		super(repo);
		super.sort(RevSort.TOPO, true);
	}

	@Override
	public void sort(final RevSort s, final boolean use) {
		if (s == RevSort.TOPO && !use)
			throw new IllegalArgumentException("Topological sort required.");
		super.sort(s, use);
	}

	@Override
	protected RevCommit createCommit(final AnyObjectId id) {
		return new PlotCommit(id);
	}
}
