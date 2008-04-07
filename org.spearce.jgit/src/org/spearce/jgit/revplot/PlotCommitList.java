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

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.spearce.jgit.revwalk.RevCommitList;
import org.spearce.jgit.revwalk.RevWalk;

/**
 * An ordered list of {@link PlotCommit} subclasses.
 * <p>
 * Commits are allocated into lanes as they enter the list, based upon their
 * connections between descendant (child) commits and ancestor (parent) commits.
 * <p>
 * The source of the list must be a {@link PlotWalk} and {@link #fillTo(int)}
 * must be used to populate the list.
 * 
 * @param <L>
 *            type of lane used by the application.
 */
public class PlotCommitList<L extends PlotLane> extends
		RevCommitList<PlotCommit<L>> {
	static final int MAX_LENGTH = 25;

	private int lanesAllocated;

	private final TreeSet<Integer> freeLanes = new TreeSet<Integer>();

	private HashSet<PlotLane> activeLanes = new HashSet<PlotLane>(32);

	@Override
	public void source(final RevWalk w) {
		if (!(w instanceof PlotWalk))
			throw new ClassCastException("Not a " + PlotWalk.class.getName());
		super.source(w);
	}

	/**
	 * Find the set of lanes passing through a commit's row.
	 * <p>
	 * Lanes passing through a commit are lanes that the commit is not directly
	 * on, but that need to travel through this commit to connect a descendant
	 * (child) commit to an ancestor (parent) commit. Typically these lanes will
	 * be drawn as lines in the passed commit's box, and the passed commit won't
	 * appear to be connected to those lines.
	 * <p>
	 * This method modifies the passed collection by adding the lanes in any
	 * order.
	 * 
	 * @param currCommit
	 *            the commit the caller needs to get the lanes from.
	 * @param result
	 *            collection to add the passing lanes into.
	 */
	public void findPassingThrough(final PlotCommit<L> currCommit,
			final Collection<L> result) {
		for (final PlotLane p : currCommit.passingLanes)
			result.add((L) p);
	}

	@Override
	protected void enter(final int index, final PlotCommit<L> currCommit) {
		setupChildren(currCommit);

		final int nChildren = currCommit.getChildCount();
		if (nChildren == 0)
			return;

		if (nChildren == 1 && currCommit.children[0].getParentCount() < 2) {
			// Only one child, child has only us as their parent.
			// Stay in the same lane as the child.
			//
			final PlotCommit c = currCommit.children[0];
			if (c.lane == null) {
				// Hmmph. This child must be the first along this lane.
				//
				c.lane = nextFreeLane();
				activeLanes.add(c.lane);
			}

			for (int r = index - 1; r >= 0; r--) {
				final PlotCommit rObj = get(r);
				if (rObj == c)
					break;
				rObj.addPassingLane(c.lane);
			}
			currCommit.lane = c.lane;
			currCommit.lane.parent = currCommit;
		} else {
			// More than one child, or our child is a merge.
			// Use a different lane.
			//

			for (int i = 0; i < nChildren; i++) {
				final PlotCommit c = currCommit.children[i];
				if (activeLanes.remove(c.lane)) {
					recycleLane((L) c.lane);
					freeLanes.add(Integer.valueOf(c.lane.position));
				}
			}

			currCommit.lane = nextFreeLane();
			currCommit.lane.parent = currCommit;
			activeLanes.add(currCommit.lane);

			int remaining = nChildren;
			for (int r = index - 1; r >= 0; r--) {
				final PlotCommit rObj = get(r);
				if (currCommit.isChild(rObj)) {
					if (--remaining == 0)
						break;
				}
				rObj.addPassingLane(currCommit.lane);
			}
		}
	}

	private void setupChildren(final PlotCommit<L> currCommit) {
		final int nParents = currCommit.getParentCount();
		for (int i = 0; i < nParents; i++)
			((PlotCommit) currCommit.getParent(i)).addChild(currCommit);
	}

	private PlotLane nextFreeLane() {
		final PlotLane p = createLane();
		if (freeLanes.isEmpty()) {
			p.position = lanesAllocated++;
		} else {
			final Integer min = freeLanes.first();
			p.position = min.intValue();
			freeLanes.remove(min);
		}
		return p;
	}

	protected L createLane() {
		return (L) new PlotLane();
	}

	protected void recycleLane(final L lane) {
		// Nothing.
	}
}
