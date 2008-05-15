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
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Specialized subclass of RevWalk to include trees, blobs and tags.
 * <p>
 * Unlike RevWalk this subclass is able to remember starting roots that include
 * annotated tags, or arbitrary trees or blobs. Once commit generation is
 * complete and all commits have been popped by the application, individual
 * annotated tag, tree and blob objects can be popped through the additional
 * method {@link #nextObject()}.
 * <p>
 * Tree and blob objects reachable from interesting commits are automatically
 * scheduled for inclusion in the results of {@link #nextObject()}, returning
 * each object exactly once. Objects are sorted and returned according to the
 * the commits that reference them and the order they appear within a tree.
 * Ordering can be affected by changing the {@link RevSort} used to order the
 * commits that are returned first.
 */
public class ObjectWalk extends RevWalk {
	private static final int SEEN_OR_UNINTERESTING = SEEN | UNINTERESTING;

	private final TreeWalk treeWalk;

	private BlockObjQueue objects;

	private RevTree currentTree;

	private boolean fromTreeWalk;

	private boolean enterSubtree;

	/**
	 * Create a new revision and object walker for a given repository.
	 * 
	 * @param repo
	 *            the repository the walker will obtain data from.
	 */
	public ObjectWalk(final Repository repo) {
		super(repo);
		treeWalk = new TreeWalk(repo);
		objects = new BlockObjQueue();
	}

	/**
	 * Mark an object or commit to start graph traversal from.
	 * <p>
	 * Callers are encouraged to use {@link RevWalk#parseAny(AnyObjectId)}
	 * instead of {@link RevWalk#lookupAny(AnyObjectId, int)}, as this method
	 * requires the object to be parsed before it can be added as a root for the
	 * traversal.
	 * <p>
	 * The method will automatically parse an unparsed object, but error
	 * handling may be more difficult for the application to explain why a
	 * RevObject is not actually valid. The object pool of this walker would
	 * also be 'poisoned' by the invalid RevObject.
	 * <p>
	 * This method will automatically call {@link RevWalk#markStart(RevCommit)}
	 * if passed RevCommit instance, or a RevTag that directly (or indirectly)
	 * references a RevCommit.
	 * 
	 * @param o
	 *            the object to start traversing from. The object passed must be
	 *            from this same revision walker.
	 * @throws MissingObjectException
	 *             the object supplied is not available from the object
	 *             database. This usually indicates the supplied object is
	 *             invalid, but the reference was constructed during an earlier
	 *             invocation to {@link RevWalk#lookupAny(AnyObjectId, int)}.
	 * @throws IncorrectObjectTypeException
	 *             the object was not parsed yet and it was discovered during
	 *             parsing that it is not actually the type of the instance
	 *             passed in. This usually indicates the caller used the wrong
	 *             type in a {@link RevWalk#lookupAny(AnyObjectId, int)} call.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public void markStart(RevObject o) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		while (o instanceof RevTag) {
			addObject(o);
			o = ((RevTag) o).getObject();
			parse(o);
		}

		if (o instanceof RevCommit)
			super.markStart((RevCommit) o);
		else
			addObject(o);
	}

	/**
	 * Mark an object to not produce in the output.
	 * <p>
	 * Uninteresting objects denote not just themselves but also their entire
	 * reachable chain, back until the merge base of an uninteresting commit and
	 * an otherwise interesting commit.
	 * <p>
	 * Callers are encouraged to use {@link RevWalk#parseAny(AnyObjectId)}
	 * instead of {@link RevWalk#lookupAny(AnyObjectId, int)}, as this method
	 * requires the object to be parsed before it can be added as a root for the
	 * traversal.
	 * <p>
	 * The method will automatically parse an unparsed object, but error
	 * handling may be more difficult for the application to explain why a
	 * RevObject is not actually valid. The object pool of this walker would
	 * also be 'poisoned' by the invalid RevObject.
	 * <p>
	 * This method will automatically call {@link RevWalk#markStart(RevCommit)}
	 * if passed RevCommit instance, or a RevTag that directly (or indirectly)
	 * references a RevCommit.
	 * 
	 * @param o
	 *            the object to start traversing from. The object passed must be
	 * @throws MissingObjectException
	 *             the object supplied is not available from the object
	 *             database. This usually indicates the supplied object is
	 *             invalid, but the reference was constructed during an earlier
	 *             invocation to {@link RevWalk#lookupAny(AnyObjectId, int)}.
	 * @throws IncorrectObjectTypeException
	 *             the object was not parsed yet and it was discovered during
	 *             parsing that it is not actually the type of the instance
	 *             passed in. This usually indicates the caller used the wrong
	 *             type in a {@link RevWalk#lookupAny(AnyObjectId, int)} call.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public void markUninteresting(RevObject o) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		while (o instanceof RevTag) {
			o.flags |= UNINTERESTING;
			o = ((RevTag) o).getObject();
			parse(o);
		}

		if (o instanceof RevCommit)
			super.markUninteresting((RevCommit) o);
		else if (o instanceof RevTree)
			markTreeUninteresting((RevTree) o);
		else
			o.flags |= UNINTERESTING;
	}

	@Override
	public RevCommit next() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		for (;;) {
			final RevCommit r = super.next();
			if (r == null)
				return null;
			if ((r.flags & UNINTERESTING) != 0) {
				markTreeUninteresting(r.getTree());
				if (getRevSort().contains(RevSort.BOUNDARY))
					return r;
				continue;
			}
			objects.add(r.getTree());
			return r;
		}
	}

	/**
	 * Pop the next most recent object.
	 * 
	 * @return next most recent object; null if traversal is over.
	 * @throws MissingObjectException
	 *             one or or more of the next objects are not available from the
	 *             object database, but were thought to be candidates for
	 *             traversal. This usually indicates a broken link.
	 * @throws IncorrectObjectTypeException
	 *             one or or more of the objects in a tree do not match the type
	 *             indicated.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public RevObject nextObject() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		fromTreeWalk = false;

		if (enterSubtree) {
			treeWalk.enterSubtree();
			enterSubtree = false;
		}

		while (treeWalk.next()) {
			final FileMode mode = treeWalk.getFileMode(0);
			final int sType = mode.getObjectType();

			switch (sType) {
			case Constants.OBJ_BLOB: {
				final RevObject o = lookupAny(treeWalk.getObjectId(0), sType);
				if ((o.flags & SEEN_OR_UNINTERESTING) != 0)
					continue;
				o.flags |= SEEN;
				fromTreeWalk = true;
				return o;
			}
			case Constants.OBJ_TREE: {
				final RevObject o = lookupAny(treeWalk.getObjectId(0), sType);
				if ((o.flags & SEEN_OR_UNINTERESTING) != 0)
					continue;
				o.flags |= SEEN;
				enterSubtree = true;
				fromTreeWalk = true;
				return o;
			}
			default:
				if (FileMode.GITLINK.equals(mode))
					continue;
				throw new CorruptObjectException("Invalid mode " + mode
						+ " for " + treeWalk.getObjectId(0) + " "
						+ treeWalk.getPathString() + " in " + currentTree + ".");
			}
		}

		for (;;) {
			final RevObject o = objects.next();
			if (o == null)
				return null;
			if ((o.flags & SEEN_OR_UNINTERESTING) != 0)
				continue;
			o.flags |= SEEN;
			if (o instanceof RevTree) {
				currentTree = (RevTree) o;
				treeWalk.reset(new ObjectId[] { currentTree });
			}
			return o;
		}
	}

	/**
	 * Verify all interesting objects are available, and reachable.
	 * <p>
	 * Callers should populate starting points and ending points with
	 * {@link #markStart(RevObject)} and {@link #markUninteresting(RevObject)}
	 * and then use this method to verify all objects between those two points
	 * exist in the repository and are readable.
	 * <p>
	 * This method returns successfully if everything is connected; it throws an
	 * exception if there is a connectivity problem. The exception message
	 * provides some detail about the connectivity failure.
	 * 
	 * @throws MissingObjectException
	 *             one or or more of the next objects are not available from the
	 *             object database, but were thought to be candidates for
	 *             traversal. This usually indicates a broken link.
	 * @throws IncorrectObjectTypeException
	 *             one or or more of the objects in a tree do not match the type
	 *             indicated.
	 * @throws IOException
	 *             a pack file or loose object could not be read.
	 */
	public void checkConnectivity() throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		for (;;) {
			final RevCommit c = next();
			if (c == null)
				break;
		}
		for (;;) {
			final RevObject o = nextObject();
			if (o == null)
				break;
			if (o instanceof RevBlob && !db.hasObject(o))
				throw new MissingObjectException(o, Constants.TYPE_BLOB);
		}
	}

	/**
	 * Get the current object's complete path.
	 * <p>
	 * This method is not very efficient and is primarily meant for debugging
	 * and final output generation. Applications should try to avoid calling it,
	 * and if invoked do so only once per interesting entry, where the name is
	 * absolutely required for correct function.
	 * 
	 * @return complete path of the current entry, from the root of the
	 *         repository. If the current entry is in a subtree there will be at
	 *         least one '/' in the returned string. Null if the current entry
	 *         has no path, such as for annotated tags or root level trees.
	 */
	public String getPathString() {
		return fromTreeWalk ? treeWalk.getPathString() : null;
	}

	@Override
	public void dispose() {
		super.dispose();
		objects = new BlockObjQueue();
		enterSubtree = false;
		currentTree = null;
	}

	@Override
	protected void reset(final int retainFlags) {
		super.reset(retainFlags);
		objects = new BlockObjQueue();
		enterSubtree = false;
	}

	private void addObject(final RevObject o) {
		if ((o.flags & SEEN) == 0) {
			o.flags |= SEEN;
			objects.add(o);
		}
	}

	private void markTreeUninteresting(final RevTree tree)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		if ((tree.flags & UNINTERESTING) != 0)
			return;
		tree.flags |= UNINTERESTING;

		treeWalk.reset(new ObjectId[] { tree });
		while (treeWalk.next()) {
			final FileMode mode = treeWalk.getFileMode(0);
			final int sType = mode.getObjectType();

			switch (sType) {
			case Constants.OBJ_BLOB: {
				final ObjectId sid = treeWalk.getObjectId(0);
				lookupAny(sid, sType).flags |= UNINTERESTING;
				continue;
			}
			case Constants.OBJ_TREE: {
				final ObjectId sid = treeWalk.getObjectId(0);
				final RevObject subtree = lookupAny(sid, sType);
				if ((subtree.flags & UNINTERESTING) == 0) {
					subtree.flags |= UNINTERESTING;
					treeWalk.enterSubtree();
				}
				continue;
			}
			default:
				if (FileMode.GITLINK.equals(mode))
					continue;
				throw new CorruptObjectException("Invalid mode " + mode
						+ " for " + treeWalk.getObjectId(0) + " "
						+ treeWalk.getPathString() + " in " + tree + ".");
			}
		}
	}
}
