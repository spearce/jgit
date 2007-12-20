/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The topological walker traverses the commit graph and
 * returns it in topological order. Topological order is
 * useful for displaying graphs of more than one branch.
 */
public class TopologicalWalker extends Walker {

		@SuppressWarnings("unchecked")
		Map<ObjectId, ObjectId> collected = new ObjectIdMap(new HashMap<ObjectId,ObjectId>());
		Map<ObjectId, Date> commitTime = new ObjectIdMap(new HashMap<ObjectId,ObjectId>());

		TopologicalSorter<ObjectId> topoSorter;
		final boolean returnAll;
		
		/**
		 * @return true if all commit should be returned rather than being filtered.
		 */
		public boolean isReturnAll() {
			return returnAll;
		}

		protected TopologicalSorter<ObjectId>.Lane getLane(ObjectId id) {
			return topoSorter.lane.get(id);
		}

		protected TopologicalWalker(final Repository repostory, Commit[] starts,
				String[] relativeResourceName, boolean leafIsBlob,
				boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId, final boolean returnAll) {
			super(repostory, starts, relativeResourceName, leafIsBlob,
					followMainOnly, merges, activeDiffLeafId);
			this.returnAll = returnAll;
			topoSorter = new TopologicalSorter<ObjectId>() {;
				@Override
				protected boolean filter(ObjectId element) {
					return returnAll ? true : collected.containsKey(element);
				}

				@Override
				public int size() {
					return returnAll ? super.size() : collected.size();
				}
			};
			topoSorter.setComparator(new Comparator<ObjectId>() {
				public int compare(ObjectId i1, ObjectId i2) {
					if (i1 == i2)
						return 0;
					if (i1 == null)
						return -1;
					if (i2 == null)
						return 1;

					if (i1.equals(i2))
						return 0;

					Date when1 = commitTime.get(i1);
					Date when2 = commitTime.get(i2);
					if (when1 == null) {
						if (when2 == null)
							return i1.compareTo(i2);
						return 1;
					}
					if (when2 == null)
						return -1;
					int c = when2.compareTo(when1);
					if (c == 0)
						return i1.compareTo(i2);
					return c;
				}
			});
		}

		@Override
		protected void record(ObjectId pred, ObjectId succ) {
			if (pred!=null) {
				if (succ != null)
					topoSorter.put(new TopologicalSorter.Edge<ObjectId>(pred, succ));
				// else topoSorter.put(pred);
			} else {
				topoSorter.put(succ);
				try {
					collectSortOrder(succ, repository.mapCommit(succ));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (pred != null)
				collectSortOrder(pred, null);
			if (succ != null)
				collectSortOrder(succ, null);
		}

		protected void collect(Commit commit, int count, int breadth) {
//			System.out.println("Got: "+count+" "+commit.getCommitId());
			ObjectId commitId = commit.getCommitId();
			if (commitId == null)
				commitId = ObjectId.zeroId();
			collected.put(commitId, commitId);
			collectSortOrder(commitId, commit);
		}

		private void collectSortOrder(ObjectId commitId, Commit commit) {
			if (commitId.equals(ObjectId.zeroId()))
				commitTime.put(commitId, new Date(Long.MAX_VALUE));
			else
				if (commitId.equals(starts[0].getCommitId()))
					commitTime.put(commitId, new Date(Long.MAX_VALUE-1));
				else
					if (commit != null)
						commitTime.put(commitId, commit.getAuthor().getWhen());
		}

		protected boolean isCancelled() {
			return false;
		}

		public Collection collectHistory() {
			super.collectHistory();
			return topoSorter.getEntries();
		}
}
