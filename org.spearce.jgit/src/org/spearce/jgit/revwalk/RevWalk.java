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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.errors.RevWalkException;
import org.spearce.jgit.errors.StopWalkException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectIdMap;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.filter.RevFilter;

/**
 * Walks a commit graph and produces the matching commits in order.
 * <p>
 * A RevWalk instance can only be used once to generate results. Running a
 * second time requires creating a new RevWalk instance, or invoking
 * {@link #reset()} before starting again. Resetting an existing instance may be
 * faster for some applications as commit body parsing can be avoided on the
 * later invocations.
 * <p>
 * RevWalk instances are not thread-safe. Applications must either restrict
 * usage of a RevWalk instance to a single thread, or implement their own
 * synchronization at a higher level.
 * <p>
 * Multiple simultaneous RevWalk instances per {@link Repository} are permitted,
 * even from concurrent threads. Equality of {@link RevCommit}s from two
 * different RevWalk instances is never true, even if their {@link ObjectId}s
 * are equal (and thus they describe the same commit).
 * <p>
 * The offered iterator is over the list of RevCommits described by the
 * configuration of this instance. Applications should restrict themselves to
 * using either the provided Iterator or {@link #next()}, but never use both on
 * the same RevWalk at the same time. The Iterator may buffer RevCommits, while
 * {@link #next()} does not.
 */
public class RevWalk implements Iterable<RevCommit> {
	/**
	 * Set on objects whose important header data has been loaded.
	 * <p>
	 * For a RevCommit this indicates we have pulled apart the tree and parent
	 * references from the raw bytes available in the repository and translated
	 * those to our own local RevTree and RevCommit instances. The raw buffer is
	 * also available for message and other header filtering.
	 * <p>
	 * For a RevTag this indicates we have pulled part the tag references to
	 * find out who the tag refers to, and what that object's type is.
	 */
	static final int PARSED = 1 << 0;

	/**
	 * Set on RevCommit instances added to our {@link #pending} queue.
	 * <p>
	 * We use this flag to avoid adding the same commit instance twice to our
	 * queue, especially if we reached it by more than one path.
	 */
	static final int SEEN = 1 << 1;

	/**
	 * Set on RevCommit instances the caller does not want output.
	 * <p>
	 * We flag commits as uninteresting if the caller does not want commits
	 * reachable from a commit given to {@link #markUninteresting(RevCommit)}.
	 * This flag is always carried into the commit's parents and is a key part
	 * of the "rev-list B --not A" feature; A is marked UNINTERESTING.
	 */
	static final int UNINTERESTING = 1 << 2;

	/** Number of flag bits we keep internal for our own use. See above flags. */
	static final int RESERVED_FLAGS = 3;

	/** Flags we must automatically carry from a child to a parent commit. */
	private static final int CARRY_MASK = UNINTERESTING;

	final Repository db;

	private final ObjectIdMap<RevObject> objects;

	private int nextFlagBit = RESERVED_FLAGS;

	private final ArrayList<RevCommit> roots;

	private final DateRevQueue pending;

	private RevFilter filter;

	/**
	 * Create a new revision walker for a given repository.
	 * 
	 * @param repo
	 *            the repository the walker will obtain data from.
	 */
	public RevWalk(final Repository repo) {
		db = repo;
		objects = new ObjectIdMap<RevObject>(new HashMap());
		roots = new ArrayList<RevCommit>();
		pending = new DateRevQueue();
		filter = RevFilter.ALL;
	}

	/**
	 * Mark a commit to start graph traversal from.
	 * <p>
	 * Callers are encouraged to use {@link #parseCommit(ObjectId)} to obtain
	 * the commit reference, rather than {@link #lookupCommit(ObjectId)}, as
	 * this method requires the commit to be parsed before it can be added as a
	 * root for the traversal.
	 * <p>
	 * The method will automatically parse an unparsed commit, but error
	 * handling may be more difficult for the application to explain why a
	 * RevCommit is not actually a commit. The object pool of this walker would
	 * also be 'poisoned' by the non-commit RevCommit.
	 * 
	 * @param c
	 *            the commit to start traversing from. The commit passed must be
	 *            from this same revision walker.
	 * @throws MissingObjectException
	 *             the commit supplied is not available from the object
	 *             database. This usually indicates the supplied commit is
	 *             invalid, but the reference was constructed during an earlier
	 *             invocation to {@link #lookupCommit(ObjectId)}.
	 * @throws IncorrectObjectTypeException
	 *             the object was not parsed yet and it was discovered during
	 *             parsing that it is not actually a commit. This usually
	 *             indicates the caller supplied a non-commit SHA-1 to
	 *             {@link #lookupCommit(ObjectId)}.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public void markStart(final RevCommit c) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if ((c.flags & SEEN) != 0)
			return;
		if ((c.flags & PARSED) == 0)
			c.parse(this);
		c.flags |= SEEN;
		roots.add(c);
		pending.add(c);
	}

	/**
	 * Mark a commit to not produce in the output.
	 * <p>
	 * Uninteresting commits denote not just themselves but also their entire
	 * ancestry chain, back until the merge base of an uninteresting commit and
	 * an otherwise interesting commit.
	 * <p>
	 * Callers are encouraged to use {@link #parseCommit(ObjectId)} to obtain
	 * the commit reference, rather than {@link #lookupCommit(ObjectId)}, as
	 * this method requires the commit to be parsed before it can be added as a
	 * root for the traversal.
	 * <p>
	 * The method will automatically parse an unparsed commit, but error
	 * handling may be more difficult for the application to explain why a
	 * RevCommit is not actually a commit. The object pool of this walker would
	 * also be 'poisoned' by the non-commit RevCommit.
	 * 
	 * @param c
	 *            the commit to start traversing from. The commit passed must be
	 *            from this same revision walker.
	 * @throws MissingObjectException
	 *             the commit supplied is not available from the object
	 *             database. This usually indicates the supplied commit is
	 *             invalid, but the reference was constructed during an earlier
	 *             invocation to {@link #lookupCommit(ObjectId)}.
	 * @throws IncorrectObjectTypeException
	 *             the object was not parsed yet and it was discovered during
	 *             parsing that it is not actually a commit. This usually
	 *             indicates the caller supplied a non-commit SHA-1 to
	 *             {@link #lookupCommit(ObjectId)}.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public void markUninteresting(final RevCommit c)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		c.flags |= UNINTERESTING;
		markStart(c);
	}

	/**
	 * Pop the next most recent commit.
	 * 
	 * @return next most recent commit; null if traversal is over.
	 * @throws MissingObjectException
	 *             one or or more of the next commit's parents are not available
	 *             from the object database, but were thought to be candidates
	 *             for traversal. This usually indicates a broken link.
	 * @throws IncorrectObjectTypeException
	 *             one or or more of the next commit's parents are not actually
	 *             commit objects.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		try {
			for (;;) {
				final RevCommit c = pending.pop();
				if (c == null)
					return null;

				final int carry = c.flags & CARRY_MASK;
				for (final RevCommit p : c.parents) {
					p.flags |= carry;
					if ((p.flags & SEEN) != 0)
						continue;
					if ((p.flags & PARSED) == 0)
						p.parse(this);
					p.flags |= SEEN;
					pending.add(p);
				}

				if ((c.flags & UNINTERESTING) != 0) {
					if (pending.everbodyHasFlag(UNINTERESTING))
						throw StopWalkException.INSTANCE;
					continue;
				}

				if (!filter.include(this, c))
					continue;

				return c;
			}
		} catch (StopWalkException swe) {
			pending.clear();
			return null;
		}
	}

	/**
	 * Get the currently configured commit filter.
	 * 
	 * @return the current filter. Never null as a filter is always needed.
	 */
	public RevFilter getRevFilter() {
		return filter;
	}

	/**
	 * Set the commit filter for this walker.
	 * <p>
	 * Multiple filters may be combined by constructing an arbitrary tree of
	 * <code>AndRevFilter</code> or <code>OrRevFilter</code> instances to
	 * describe the boolean expression required by the application. Custom
	 * filter implementations may also be constructed by applications.
	 * <p>
	 * Note that filters are not thread-safe and may not be shared by concurrent
	 * RevWalk instances. Every RevWalk must be supplied its own unique filter,
	 * unless the filter implementation specifically states it is (and always
	 * will be) thread-safe. Callers may use {@link RevFilter#clone()} to create
	 * a unique filter tree for this RevWalk instance.
	 * 
	 * @param newFilter
	 *            the new filter. If null the special {@link RevFilter#ALL}
	 *            filter will be used instead, as it matches every commit.
	 * @see org.spearce.jgit.revwalk.filter.AndRevFilter
	 * @see org.spearce.jgit.revwalk.filter.OrRevFilter
	 */
	public void setRevFilter(final RevFilter newFilter) {
		filter = newFilter != null ? newFilter : RevFilter.ALL;
	}

	/**
	 * Locate a reference to a tree without loading it.
	 * <p>
	 * The tree may or may not exist in the repository. It is impossible to tell
	 * from this method's return value.
	 * 
	 * @param id
	 *            name of the tree object.
	 * @return reference to the tree object. Never null.
	 */
	public RevTree lookupTree(final ObjectId id) {
		RevTree c = (RevTree) objects.get(id);
		if (c == null) {
			c = new RevTree(id);
			objects.put(c.id, c);
		}
		return c;
	}

	/**
	 * Locate a reference to a commit without loading it.
	 * <p>
	 * The commit may or may not exist in the repository. It is impossible to
	 * tell from this method's return value.
	 * 
	 * @param id
	 *            name of the commit object.
	 * @return reference to the commit object. Never null.
	 */
	public RevCommit lookupCommit(final ObjectId id) {
		RevCommit c = (RevCommit) objects.get(id);
		if (c == null) {
			c = new RevCommit(id);
			objects.put(c.id, c);
		}
		return c;
	}

	/**
	 * Locate a reference to any object without loading it.
	 * <p>
	 * The object may or may not exist in the repository. It is impossible to
	 * tell from this method's return value.
	 * 
	 * @param id
	 *            name of the object.
	 * @param type
	 *            type of the object. Must be a valid Git object type.
	 * @return reference to the object. Never null.
	 */
	public RevObject lookupAny(final ObjectId id, final int type) {
		RevObject r = objects.get(id);
		if (r == null) {
			switch (type) {
			case Constants.OBJ_COMMIT:
				r = new RevCommit(id);
				break;
			case Constants.OBJ_TREE:
				r = new RevTree(id);
				break;
			case Constants.OBJ_BLOB:
				r = new RevBlob(id);
				break;
			case Constants.OBJ_TAG:
				r = new RevTag(id);
				break;
			default:
				throw new IllegalArgumentException("invalid git type: " + type);
			}
			objects.put(r.id, r);
		}
		return r;
	}

	/**
	 * Locate a reference to a commit and immediately parse its content.
	 * <p>
	 * Unlike {@link #lookupCommit(ObjectId)} this method only returns
	 * successfully if the commit object exists, is verified to be a commit, and
	 * was parsed without error.
	 * 
	 * @param id
	 *            name of the commit object.
	 * @return reference to the commit object. Never null.
	 * @throws MissingObjectException
	 *             the supplied commit does not exist.
	 * @throws IncorrectObjectTypeException
	 *             the supplied id is not a commit or an annotated tag.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public RevCommit parseCommit(final ObjectId id)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		RevObject c = parseAny(id);
		while (c instanceof RevTag) {
			final RevTag t = ((RevTag) c);
			if ((t.flags & PARSED) == 0)
				t.parse(this);
			c = t.getObject();
		}
		return (RevCommit) c;
	}

	/**
	 * Locate a reference to any object and immediately parse its content.
	 * <p>
	 * This method only returns successfully if the object exists and was parsed
	 * without error. Parsing an object can be expensive as the type must be
	 * determined. For blobs this may mean the blob content was unpacked
	 * unnecessarily, and thrown away.
	 * 
	 * @param id
	 *            name of the object.
	 * @return reference to the object. Never null.
	 * @throws MissingObjectException
	 *             the supplied does not exist.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public RevObject parseAny(final ObjectId id) throws MissingObjectException,
			IOException {
		RevObject r = objects.get(id);
		if (r == null) {
			final ObjectLoader ldr = db.openObject(id);
			if (ldr == null)
				throw new MissingObjectException(id, "unknown");
			final byte[] data = ldr.getBytes();
			final int type = ldr.getType();
			switch (type) {
			case Constants.OBJ_COMMIT: {
				final RevCommit c = new RevCommit(ldr.getId());
				c.parseCanonical(this, data);
				r = c;
				break;
			}
			case Constants.OBJ_TREE: {
				r = new RevTree(ldr.getId());
				break;
			}
			case Constants.OBJ_BLOB: {
				r = new RevBlob(ldr.getId());
				break;
			}
			case Constants.OBJ_TAG: {
				final RevTag t = new RevTag(ldr.getId());
				t.parseCanonical(this, data);
				r = t;
				break;
			}
			default:
				throw new IllegalArgumentException("Bad object type: " + type);
			}
			objects.put(r.getId(), r);
		}
		return r;
	}

	/**
	 * Create a new flag for application use during walking.
	 * <p>
	 * Applications are only assured to be able to create 24 unique flags on any
	 * given revision walker instance. Any flags beyond 24 are offered only if
	 * the implementation has extra free space within its internal storage.
	 * 
	 * @param name
	 *            description of the flag, primarily useful for debugging.
	 * @return newly constructed flag instance.
	 * @throws IllegalArgumentException
	 *             too many flags have been reserved on this revision walker.
	 */
	public RevFlag newFlag(final String name) {
		if (nextFlagBit == 32)
			throw new IllegalArgumentException(32 - RESERVED_FLAGS
					+ " flags already created.");
		return new RevFlag(name, 1 << nextFlagBit++);
	}

	/** Resets internal state and allows this instance to be used again. */
	public void reset() {
		pending.clear();

		for (final RevCommit c : roots) {
			if ((c.flags & SEEN) == 0)
				continue;
			c.flags &= PARSED;
			pending.add(c);
		}

		for (;;) {
			final RevCommit c = pending.pop();
			if (c == null)
				break;
			for (final RevCommit p : c.parents) {
				if ((p.flags & SEEN) == 0)
					continue;
				p.flags &= PARSED;
				pending.add(p);
			}
		}

		roots.clear();
	}

	/**
	 * Returns an Iterator over the commits of this walker.
	 * <p>
	 * The returned iterator is only useful for one walk. If this RevWalk gets
	 * reset a new iterator must be obtained to walk over the new results.
	 * <p>
	 * Applications must not use both the Iterator and the {@link #next()} API
	 * at the same time. Pick one API and use that for the entire walk.
	 * <p>
	 * If a checked exception is thrown during the walk (see {@link #next()})
	 * it is rethrown from the Iterator as a {@link RevWalkException}.
	 * 
	 * @return an iterator over this walker's commits.
	 * @see RevWalkException
	 */
	public Iterator<RevCommit> iterator() {
		final RevCommit first;
		try {
			first = RevWalk.this.next();
		} catch (MissingObjectException e) {
			throw new RevWalkException(e);
		} catch (IncorrectObjectTypeException e) {
			throw new RevWalkException(e);
		} catch (IOException e) {
			throw new RevWalkException(e);
		}

		return new Iterator<RevCommit>() {
			RevCommit next = first;

			public boolean hasNext() {
				return next != null;
			}

			public RevCommit next() {
				try {
					final RevCommit r = next;
					next = RevWalk.this.next();
					return r;
				} catch (MissingObjectException e) {
					throw new RevWalkException(e);
				} catch (IncorrectObjectTypeException e) {
					throw new RevWalkException(e);
				} catch (IOException e) {
					throw new RevWalkException(e);
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
