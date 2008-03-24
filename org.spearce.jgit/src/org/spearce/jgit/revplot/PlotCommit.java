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
import org.spearce.jgit.revwalk.RevCommit;

/**
 * A commit reference to a commit in the DAG.
 * 
 * @param <L>
 *            type of lane being used by the plotter.
 * @see PlotCommitList
 */
public class PlotCommit<L extends PlotLane> extends RevCommit {
	static final PlotCommit[] NO_CHILDREN = {};

	static final PlotLane[] NO_LANES = {};

	PlotLane[] passingLanes;

	PlotLane lane;

	PlotCommit[] children;

	/**
	 * Create a new commit.
	 * 
	 * @param id
	 *            the identity of this commit.
	 */
	protected PlotCommit(final AnyObjectId id) {
		super(id);
		passingLanes = NO_LANES;
		children = NO_CHILDREN;
	}

	void addPassingLane(final PlotLane c) {
		final int cnt = passingLanes.length;
		if (cnt == 0)
			passingLanes = new PlotLane[] { c };
		else if (cnt == 1)
			passingLanes = new PlotLane[] { passingLanes[0], c };
		else {
			final PlotLane[] n = new PlotLane[cnt + 1];
			System.arraycopy(passingLanes, 0, n, 0, cnt);
			n[cnt] = c;
			passingLanes = n;
		}
	}

	void addChild(final PlotCommit c) {
		final int cnt = children.length;
		if (cnt == 0)
			children = new PlotCommit[] { c };
		else if (cnt == 1)
			children = new PlotCommit[] { children[0], c };
		else {
			final PlotCommit[] n = new PlotCommit[cnt + 1];
			System.arraycopy(children, 0, n, 0, cnt);
			n[cnt] = c;
			children = n;
		}
	}

	/**
	 * Get the number of child commits listed in this commit.
	 * 
	 * @return number of children; always a positive value but can be 0.
	 */
	public final int getChildCount() {
		return children.length;
	}

	/**
	 * Get the nth child from this commit's child list.
	 * 
	 * @param nth
	 *            child index to obtain. Must be in the range 0 through
	 *            {@link #getChildCount()}-1.
	 * @return the specified child.
	 * @throws ArrayIndexOutOfBoundsException
	 *             an invalid child index was specified.
	 */
	public final PlotCommit getChild(final int nth) {
		return children[nth];
	}

	/**
	 * Determine if the given commit is a child (descendant) of this commit.
	 * 
	 * @param c
	 *            the commit to test.
	 * @return true if the given commit built on top of this commit.
	 */
	public final boolean isChild(final PlotCommit c) {
		for (final PlotCommit a : children)
			if (a == c)
				return true;
		return false;
	}

	/**
	 * Obtain the lane this commit has been plotted into.
	 * 
	 * @return the assigned lane for this commit.
	 */
	public final L getLane() {
		return (L) lane;
	}

	@Override
	public void reset() {
		passingLanes = NO_LANES;
		children = NO_CHILDREN;
		lane = null;
		super.reset();
	}

	/**
	 * Determine if this lane ends at this commit, but continues further.
	 * 
	 * @param lane
	 *            the lane to test.
	 * @return true if this lane is should be cut off here at this commit to
	 *         conserve lane space; false if it should continue through.
	 */
	public final boolean isLaneCutToParent(final PlotLane lane) {
		return false;
	}

	/**
	 * Determine if this lane ends at this commit, but continues further.
	 * 
	 * @param lane
	 *            the lane to test.
	 * @return true if this lane is should be cut off here at this commit to
	 *         conserve lane space; false if it should continue through.
	 */
	public final boolean isLaneCutFromChild(final PlotLane lane) {
		return false;
	}
}
