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

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.revwalk.filter.RevFilter;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.filter.TreeFilter;

/**
 * First phase of a path limited revision walk.
 * <p>
 * This generator takes the place of {@link AbstractPendingGenerator} and ties
 * the configured {@link TreeFilter} into the revision walking process.
 * <p>
 * Each commit is differenced concurrently against all of its parents to look
 * for tree entries that are interesting to the TreeFilter. If none are found
 * the commit is colored with {@link RevWalk#REWRITE}, allowing a later pass
 * implemented by {@link RewriteGenerator} to remove those colored commits from
 * the DAG.
 * 
 * @see RewriteGenerator
 */
class TreeFilterPendingGenerator extends AbstractPendingGenerator {
	private static final int PARSED = RevWalk.PARSED;

	private static final int UNINTERESTING = RevWalk.UNINTERESTING;

	private static final int REWRITE = RevWalk.REWRITE;

	private final TreeWalk pathFilter;

	TreeFilterPendingGenerator(final RevWalk w, final AbstractRevQueue p,
			final RevFilter f, final TreeFilter t) {
		super(w, p, f);
		pathFilter = new TreeWalk(w.db);
		pathFilter.setFilter(t);
		pathFilter.setRecursive(t.shouldBeRecursive());
	}

	@Override
	int outputType() {
		return super.outputType() | HAS_REWRITE | NEEDS_REWRITE;
	}

	boolean include(final RevCommit c) throws MissingObjectException,
			IncorrectObjectTypeException, IOException, CorruptObjectException {
		// Reset the tree filter to scan this commit and parents.
		//
		final RevCommit[] pList = c.parents;
		final int nParents = pList.length;
		final TreeWalk tw = pathFilter;
		final ObjectId[] trees = new ObjectId[nParents + 1];
		for (int i = 0; i < nParents; i++) {
			final RevCommit p = c.parents[i];
			if ((p.flags & PARSED) == 0)
				p.parse(walker);
			trees[i] = p.getTree();
		}
		trees[nParents] = c.getTree();
		tw.reset(trees);

		if (nParents == 1) {
			// We have exactly one parent. This is a very common case.
			//
			int chgs = 0, adds = 0;
			while (tw.next()) {
				chgs++;
				if (tw.getRawMode(0) == 0 && tw.getRawMode(1) != 0)
					adds++;
				else
					break; // no point in looking at this further.
			}

			if (chgs == 0) {
				// No changes, so our tree is effectively the same as
				// our parent tree. We pass the buck to our parent.
				//
				c.flags |= REWRITE;
				return false;
			} else if (chgs == adds) {
				// We added everything, so the parent may as well just
				// be an empty tree. Kill our parent, we can assume
				// it did not supply interesting changes.
				//
				c.parents = RevCommit.NO_PARENTS;
				return true;
			} else {
				// We have interesting items, but neither of the special
				// cases denoted above.
				//
				return true;
			}
		} else if (nParents == 0) {
			// We have no parents to compare against. Consider us to be
			// REWRITE only if we have no paths matching our filter.
			//
			if (tw.next())
				return true;
			c.flags |= REWRITE;
			return false;
		}

		// We are a merge commit. We can only be REWRITE if we are same
		// to _all_ parents. We may also be able to eliminate a parent if
		// it does not contribute changes to us. Such a parent may be an
		// uninteresting side branch.
		//
		final int[] chgs = new int[nParents];
		final int[] adds = new int[nParents];
		while (tw.next()) {
			final int myMode = tw.getRawMode(nParents);
			for (int i = 0; i < nParents; i++) {
				final int pMode = tw.getRawMode(i);
				if (myMode == pMode && tw.idEqual(i, nParents))
					continue;

				chgs[i]++;
				if (pMode == 0 && myMode != 0)
					adds[i]++;
			}
		}

		boolean same = false;
		boolean diff = false;
		for (int i = 0; i < nParents; i++) {
			if (chgs[i] == 0) {
				// No changes, so our tree is effectively the same as
				// this parent tree. We pass the buck to only this one
				// parent commit.
				//

				final RevCommit p = pList[i];
				if ((p.flags & UNINTERESTING) != 0) {
					// This parent was marked as not interesting by the
					// application. We should look for another parent
					// that is interesting.
					//
					same = true;
					continue;
				}

				c.flags |= REWRITE;
				c.parents = new RevCommit[] { p };
				return false;
			}

			if (chgs[i] == adds[i]) {
				// All of the differences from this parent were because we
				// added files that they did not have. This parent is our
				// "empty tree root" and thus their history is not relevant.
				// Cut our grandparents to be an empty list.
				//
				pList[i].parents = RevCommit.NO_PARENTS;
			}

			// We have an interesting difference relative to this parent.
			//
			diff = true;
		}

		if (diff && !same) {
			// We did not abort above, so we are different in at least one
			// way from all of our parents. We have to take the blame for
			// that difference.
			//
			return true;
		}

		// We are the same as all of our parents. We must keep them
		// as they are and allow those parents to flow into pending
		// for further scanning.
		//
		c.flags |= REWRITE;
		return false;
	}
}
