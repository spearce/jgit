/**
 * 
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class Walker {
	private String[] relativeResourceName;
	private boolean leafIsBlob;
	private boolean followMainOnly;
	private Repository repository;
	private ObjectId activeDiffLeafId;
	private final Commit start;
	private final Boolean merges;

	protected abstract boolean isCancelled();
	
	protected abstract void collect(Collection ret,Commit commit, int count);

	/**
	 * Create a revision walker. A revision walker traverses the revision tree
	 * and collects revisions that represents changes. The first revision is
	 * a state outside the commit and is returned as null in the list. The
	 * changes are detected in reverse order, i.e. we start with the current
	 * state as represented by activeDiffLeafId and then we list all commits
	 * that introduces a change against the older state.
	 *
	 * Consider these states
	 * CURRENT
	 * HEAD
	 * HEAD~1
	 * HEAD~2
	 * HEAD~3
	 *
	 * If the state of CURRENT and HEAD is the same, but HEAD~1 has a different
	 * state, the HEAD is listed..
	 *
	 * If the state of CURRENT and HEAD is different CURRENT is listed. The default
	 * impleementation listes it as a null commit (without a commit id).
	 *
	 * @param repostory The repository to scan
	 * @param start HEAD in this context
	 * @param relativeResourceName The path to log, split by path components
	 * @param leafIsBlob We refer to a CURRENT state which is a blob
	 * @param followMainOnly Follow the first parent only
	 * @param merges Include or eclude merges or both as a tristate Boolean.
	 * @param activeDiffLeafId a SHA-1 or null to start comparing with.2
	 */
	protected Walker(Repository repostory, Commit start,
			String[] relativeResourceName, boolean leafIsBlob,
			boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId) {
		this.repository = repostory;
		this.start = start;
		this.relativeResourceName = relativeResourceName;
		this.leafIsBlob = leafIsBlob;
		this.followMainOnly = followMainOnly;
		this.merges = merges;
		this.activeDiffLeafId = activeDiffLeafId;
	}
	
	/**
	 * Entry point for executing the collection process.
	 *
	 * @return a Collection of Commit's (default), but subclasses could return another type.
	 */
	public Collection collectHistory() {
		try {
			Commit commit = start;
			ObjectId[] initialResourceHash = new ObjectId[relativeResourceName.length];
			Arrays.fill(initialResourceHash, ObjectId.zeroId());
			if (activeDiffLeafId != null && initialResourceHash.length > 0)
				initialResourceHash[initialResourceHash.length-1] = activeDiffLeafId;
			return collectHistory(0, initialResourceHash, null, commit);
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	/* The workhorse of the collection process. This procedure is execute recursively. */
	private Collection collectHistory(int count, ObjectId[] lastResourceHash,
			TreeEntry lastEntry, Commit top) throws IOException {
		if (top == null)
			return Collections.EMPTY_LIST;
		Collection ret = new ArrayList(10000);
		Commit current = top;
		Commit previous = new Commit(repository);

		do {
			TreeEntry currentEntry = lastEntry;
			ObjectId[] currentResourceHash = new ObjectId[lastResourceHash.length];
			Tree t = current!=null ? current.getTree() : null;
			for (int i = 0; i < currentResourceHash.length; ++i) {
				TreeEntry m;
				if (t != null)
					if (i == relativeResourceName.length-1 && leafIsBlob)
						m = t.findBlobMember(relativeResourceName[i]);
					else
						m = t.findTreeMember(relativeResourceName[i]);
				else
					m = null;

				if (m != null) {
					ObjectId id = m.getId();
					currentResourceHash[i] = id;
					if (id.equals(lastResourceHash[i])) {
						while (++i < currentResourceHash.length) {
							currentResourceHash[i] = lastResourceHash[i];
						}
					} else {
						if (m instanceof Tree) {
							t = (Tree)m;
						} else {
							if (i == currentResourceHash.length - 1) {
								currentEntry = m;
							} else {
								currentEntry = null;
								while (++i < currentResourceHash.length) {
									currentResourceHash[i] = ObjectId.zeroId();
								}
							}
						}
					}
				} else {
					for (; i < currentResourceHash.length; ++i) {
						currentResourceHash[i] = ObjectId.zeroId();
					}
				}
			}
			ObjectId[] parents = current != null ? current.getParentIds() : new ObjectId[0];
			ObjectId[] pparents = previous != null ? previous.getParentIds() : new ObjectId[0];
			if (currentResourceHash.length == 0 || !currentResourceHash[currentResourceHash.length-1].equals(lastResourceHash[currentResourceHash.length-1])) {
				if (current != previous)
					if (merges == null
							|| merges.booleanValue() && pparents.length > 1
							|| !merges.booleanValue() && pparents.length <= 1)
						collect(ret, previous, count);
			}
			lastResourceHash = currentResourceHash;
			previous = current;

			// TODO: we may need to list more revisions when traversing
			// branches
			if (!followMainOnly) {
				for (int i = 1; i < parents.length; ++i) {
					ObjectId mergeParentId = parents[i];
					Commit mergeParent;
					try {
						mergeParent = repository.mapCommit(mergeParentId);
						ret.addAll(collectHistory(0, lastResourceHash,
								currentEntry, mergeParent));
						// TODO: this gets us a lot of duplicates that we need
						// to filter out
						// Leave that til we get a GUI.
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (parents.length > 0) {
				ObjectId parentId = parents[0];
				try {
					current = repository.mapCommit(parentId);
				} catch (IOException e) {
					e.printStackTrace();
					current = null;
				}
			} else
				current = null;
			if (count>=0)
				count++;
		} while (previous != null && !isCancelled());

		return ret;
	}
}