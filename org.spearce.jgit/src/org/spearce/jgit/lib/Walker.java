/**
 * 
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public abstract class Walker {
	private String[] relativeResourceName;
	private boolean leafIsBlob;
	private boolean followMainOnly;
	protected Repository repository;
	private ObjectId activeDiffLeafId;
	protected final Commit[] starts;
	private final Boolean merges;
	private Map donewith = new ObjectIdMap();
	private Collection<Todo> todo = new ArrayList<Todo>(20000);

	protected abstract boolean isCancelled();
	
	protected abstract void collect(Commit commit, int count,int breadth);
	protected abstract void record(ObjectId pred, ObjectId succ);

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
	 * @param starts HEAD in this context
	 * @param relativeResourceName The path to log, split by path components
	 * @param leafIsBlob We refer to a CURRENT state which is a blob
	 * @param followMainOnly Follow the first parent only
	 * @param merges Include or eclude merges or both as a tristate Boolean.
	 * @param activeDiffLeafId a SHA-1 or null to start comparing with.2
	 */
	protected Walker(Repository repostory, Commit[] starts,
			String[] relativeResourceName, boolean leafIsBlob,
			boolean followMainOnly, Boolean merges, ObjectId activeDiffLeafId) {
		this.repository = repostory;
		this.starts = starts;
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
		for (int i=0; i<starts.length; ++i) {
			ObjectId[] initialResourceHash = new ObjectId[relativeResourceName.length];
			Arrays.fill(initialResourceHash, ObjectId.zeroId());
			Commit initialPrevious;
			// The first commit is special. We compare it with a reference
			if (i == 0) {
				if (activeDiffLeafId != null && initialResourceHash.length > 0) {
					initialResourceHash[initialResourceHash.length-1] = activeDiffLeafId;
					initialPrevious = new Commit(repository);
					initialPrevious.setCommitId(ObjectId.zeroId());
					record(null, initialPrevious.getCommitId());
				} else {
					record(ObjectId.zeroId(), starts[i].getCommitId());
					initialPrevious = null;
				}
			} else {
				initialPrevious = null;
			}
			todo.add(new Todo(0, 0, initialResourceHash, null, starts[i], initialPrevious));
		}
		for (Iterator<Todo> ti = todo.iterator(); ti.hasNext(); ti=todo.iterator()) {
			Todo next = ti.next();
			try {
				next.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			todo.remove(next);
		}
		donewith = null; // turn over to gc
		return null;
	}

class Todo {
	private int count;
	private int breadth;
	private ObjectId[] lastResourceHash;
	private TreeEntry lastEntry;
	private Commit top;
	private Commit previous;

	@Override
	public boolean equals(Object other) {
		return top.equals(((Todo)other).top);
	}

	@Override
	public int hashCode() {
		return top.hashCode();
	}

	Todo(int count, int breadth, ObjectId[] lastResourceHash, TreeEntry lastEntry,
			Commit top, Commit previous) {
				this.count = count;
				this.breadth = breadth;
				this.previous = previous;
				this.lastResourceHash = new ObjectId[lastResourceHash.length];
				for (int i=0; i<lastResourceHash.length; ++i)
					this.lastResourceHash[i] = lastResourceHash[i];
				this.lastEntry = lastEntry;
				this.top = top;
				assert previous==null || !previous.equals(top);
	}
	void run() throws IOException {
//		System.out.println("todo.run(+"+top+"...");
		if (top == null)
			return;
		Commit current = top;

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

			if (previous != null) {
				if (currentResourceHash.length == 0 || !currentResourceHash[currentResourceHash.length-1].equals(lastResourceHash[currentResourceHash.length-1])) {
					ObjectId[] pparents = previous.getParentIds();
					if (current != previous) {
						if (merges == null
								|| merges.booleanValue() && pparents.length > 1
								|| !merges.booleanValue() && pparents.length <= 1) {
							collect(previous, count, breadth);
						}
					}
				}
			}

			record(previous!=null ? previous.getCommitId() : null, current!=null ? current.getCommitId() : null);

			if (current != null) {
				if (!followMainOnly && donewith.put(current.getCommitId(), current.getCommitId()) != null) {
					previous = current;
					break;
				}
			}

			lastResourceHash = currentResourceHash;
			previous = current;

			if (current != null) {
				ObjectId[] parents = current.getParentIds();
				if (!followMainOnly) {
					for (int i = 1; i < parents.length; ++i) {
						ObjectId mergeParentId = parents[i];
						Commit mergeParent;
						try {
							mergeParent = repository.mapCommit(mergeParentId);
							Todo newTodo = new Todo(0, breadth + 1, lastResourceHash,
									currentEntry, mergeParent, previous);
							if (!todo.contains(todo))
								todo.add(newTodo);
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
			}
			if (count>=0)
				count++;
		} while (previous != null && !isCancelled());

	}
}
}