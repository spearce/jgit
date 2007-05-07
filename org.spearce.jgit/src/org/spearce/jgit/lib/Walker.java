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

	protected abstract boolean isCancelled();
	
	protected abstract void collect(Collection ret,Commit commit, int count);

	protected Walker(Repository repostory, Commit start, String[] relativeResourceName,boolean leafIsBlob,boolean followMainOnly, ObjectId activeDiffLeafId) {
		this.repository = repostory;
		this.start = start;
		this.relativeResourceName = relativeResourceName;
		this.leafIsBlob = leafIsBlob;
		this.followMainOnly = followMainOnly;
		this.activeDiffLeafId = activeDiffLeafId;
	}
	
	public Collection collectHistory() {
		try {
			Commit commit = start;
			ObjectId[] initialResourceHash = new ObjectId[relativeResourceName.length];
			Arrays.fill(initialResourceHash, ObjectId.zeroId());
			if (activeDiffLeafId != null)
				initialResourceHash[initialResourceHash.length-1] = activeDiffLeafId;
			return collectHistory(0, initialResourceHash, null,
					repository, commit);
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	Collection collectHistory(int count, ObjectId[] lastResourceHash, TreeEntry lastEntry,
			Repository repository, Commit top) throws IOException {
		if (top == null)
			return Collections.EMPTY_LIST;
		Collection ret = new ArrayList(10000);
		Commit current = top;
		Commit previous = top;

		do {
			TreeEntry currentEntry = lastEntry;
			ObjectId[] currentResourceHash = new ObjectId[lastResourceHash.length];
			Tree t = current.getTree();
			for (int i = 0; i < currentResourceHash.length; ++i) {
				TreeEntry m;
				if (i == relativeResourceName.length-1 && leafIsBlob)
					m = t.findBlobMember(relativeResourceName[i]);
				else
					m = t.findTreeMember(relativeResourceName[i]);
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
			
			if (currentResourceHash.length == 0 || !currentResourceHash[currentResourceHash.length-1].equals(lastResourceHash[currentResourceHash.length-1])) {
				collect(ret, previous, count);
			}
			lastResourceHash = currentResourceHash;
			previous = current;

			// TODO: we may need to list more revisions when traversing
			// branches
			ObjectId[] parents = current.getParentIds();
			if (!followMainOnly) {
				for (int i = 1; i < parents.length; ++i) {
					ObjectId mergeParentId = parents[i];
					Commit mergeParent;
					try {
						mergeParent = repository.mapCommit(mergeParentId);
						ret.addAll(collectHistory(0, lastResourceHash, currentEntry, repository, 
								mergeParent));
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
		} while (current != null && !isCancelled());

		return ret;
	}
}