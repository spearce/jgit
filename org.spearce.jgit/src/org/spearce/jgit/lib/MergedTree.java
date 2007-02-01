/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.IOException;

public class MergedTree {
	public static final boolean isAdded(final TreeEntry[] ents) {
		if (ents.length == 2) {
			if (ents[0] == null && ents[1] != null)
				return true;
			else if (ents[0] != null && ents[1] != null
					&& ents[0].getClass() != ents[1].getClass())
				return true;
		}
		return false;
	}

	public static final boolean isRemoved(final TreeEntry[] ents) {
		if (ents.length == 2) {
			if (ents[0] != null && ents[1] == null)
				return true;
			else if (ents[0] != null && ents[1] != null
					&& ents[0].getClass() != ents[1].getClass())
				return true;
		}
		return false;
	}

	public static final boolean isModified(final TreeEntry[] ents) {
		if (ents.length == 2 && ents[0] != null && ents[1] != null) {
			if (ents[0].getId() == null || ents[1].getId() == null)
				return true;
			else if (ents[0].getClass() == ents[1].getClass()
					&& !ents[0].getId().equals(ents[1].getId()))
				return true;
		}
		return false;
	}

	private static final int binarySearch(final TreeEntry[] entries,
			final int width, final byte[] nameUTF8, final int nameStart,
			final int nameEnd) {
		if (entries.length == 0)
			return -1;
		int high = entries.length / width;
		int low = 0;
		do {
			final int mid = (low + high) / 2;
			final int cmp;
			int ix = mid * width;
			while (entries[ix] == null)
				ix++;
			cmp = Tree.compareNames(entries[ix].getNameUTF8(), nameUTF8,
					nameStart, nameEnd);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp == 0)
				return mid;
			else
				high = mid;
		} while (low < high);
		return -(low + 1);
	}

	private final Tree[] sources;

	private TreeEntry[] merged;

	private MergedTree[] subtrees;

	public MergedTree(final Tree[] src) throws IOException {
		if (src.length < 2)
			throw new IllegalArgumentException("At least two trees are"
					+ " required to compute a merge.");
		sources = src;
		matchByName();
	}

	public boolean isModified() {
		return isModified(sources);
	}

	public TreeEntry[] findMember(final String s) throws IOException {
		return findMember(s.getBytes(Constants.CHARACTER_ENCODING), 0);
	}

	public TreeEntry[] findMember(final byte[] s, final int offset)
			throws IOException {
		final int srcCnt = sources.length;
		int slash;
		final int p;

		for (slash = offset; slash < s.length && s[slash] != '/'; slash++) {
			// search for path component terminator
		}
		p = binarySearch(merged, srcCnt, s, offset, slash);
		if (p < 0)
			return null;

		if (slash == s.length) {
			final TreeEntry[] r = new TreeEntry[srcCnt];
			for (int j = 0, k = p * srcCnt; j < srcCnt; j++, k++)
				r[j] = merged[k];
			return r;
		}

		if (subtrees == null)
			subtrees = new MergedTree[merged.length / srcCnt];
		if (subtrees[p] == null) {
			final Tree[] subs = new Tree[srcCnt];
			for (int j = 0, k = p * srcCnt; j < srcCnt; j++, k++)
				subs[j] = merged[k] instanceof Tree ? (Tree) merged[k] : null;
			subtrees[p] = new MergedTree(subs);
		}
		return subtrees[p].findMember(s, slash + 1);
	}

	private void matchByName() throws IOException {
		final int srcCnt = sources.length;
		final int[] treeIndexes = new int[srcCnt];
		final TreeEntry[][] entries = new TreeEntry[srcCnt][];
		int pos = merged != null ? merged.length / srcCnt : 0;
		int done = 0;
		int treeId;
		TreeEntry[] newMerged;

		for (int srcId = srcCnt - 1; srcId >= 0; srcId--) {
			if (sources[srcId] != null) {
				final TreeEntry[] ents = sources[srcId].members();
				entries[srcId] = ents;
				pos = Math.max(pos, ents.length);
				if (ents.length == 0)
					done++;
			} else {
				entries[srcId] = Tree.EMPTY_TREE;
				done++;
			}
		}

		if (done == srcCnt) {
			merged = Tree.EMPTY_TREE;
			return;
		}

		newMerged = new TreeEntry[pos * srcCnt];
		for (pos = 0, treeId = 0; done < srcCnt; pos += srcCnt, treeId++) {
			byte[] minName = null;

			if ((pos + srcCnt) >= newMerged.length) {
				final TreeEntry[] t = new TreeEntry[newMerged.length * 2];
				for (int j = newMerged.length - 1; j >= 0; j--)
					t[j] = newMerged[j];
				newMerged = t;
			}

			for (int srcId = 0; srcId < srcCnt; srcId++) {
				final int ti = treeIndexes[srcId];
				final TreeEntry[] ents = entries[srcId];
				if (ti == ents.length)
					continue;

				final TreeEntry thisEntry = ents[ti];
				final int cmp = minName == null ? -1 : Tree.compareNames(
						thisEntry.getNameUTF8(), minName);

				if (cmp < 0) {
					minName = thisEntry.getNameUTF8();
					for (int j = srcId - 1; j >= 0; j--) {
						if (newMerged[pos + j] != null) {
							newMerged[pos + j] = null;
							if (treeIndexes[j]-- == entries[j].length)
								done--;
						}
					}
				}

				if (cmp <= 0) {
					newMerged[pos + srcId] = thisEntry;
					if (++treeIndexes[srcId] == ents.length)
						done++;
				}
			}
		}

		if (newMerged.length == pos)
			merged = newMerged;
		else {
			merged = new TreeEntry[pos];
			for (int j = pos - 1; j >= 0; j--)
				merged[j] = newMerged[j];
		}
	}
}
