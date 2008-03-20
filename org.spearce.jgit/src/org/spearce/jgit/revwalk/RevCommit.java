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

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;

/** A commit reference to a commit in the DAG. */
public class RevCommit extends RevObject {
	static final RevCommit[] NO_PARENTS = {};

	private static final String TYPE_COMMIT = Constants.TYPE_COMMIT;

	private RevTree tree;

	RevCommit[] parents;

	int commitTime;

	int inDegree;

	private byte[] buffer;

	/**
	 * Create a new commit reference.
	 * 
	 * @param id
	 *            object name for the commit.
	 */
	protected RevCommit(final ObjectId id) {
		super(id);
	}

	void parse(final RevWalk walk) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		final ObjectLoader ldr = walk.db.openObject(this);
		if (ldr == null)
			throw new MissingObjectException(this, TYPE_COMMIT);
		final byte[] data = ldr.getCachedBytes();
		if (Constants.OBJ_COMMIT != ldr.getType())
			throw new IncorrectObjectTypeException(this, TYPE_COMMIT);
		parseCanonical(walk, data);
	}

	void parseCanonical(final RevWalk walk, final byte[] raw) {
		tree = walk.lookupTree(fromString(raw, 5));

		final int rawSize = raw.length;
		int ptr = 46;
		RevCommit[] pList = new RevCommit[1];
		int nParents = 0;
		for (;;) {
			if (raw[ptr] != 'p')
				break;
			final RevCommit p = walk.lookupCommit(fromString(raw, ptr + 7));
			if (nParents == 0)
				pList[nParents++] = p;
			else if (nParents == 1) {
				pList = new RevCommit[] { pList[0], p };
				nParents = 2;
			} else {
				if (pList.length <= nParents) {
					RevCommit[] old = pList;
					pList = new RevCommit[pList.length + 32];
					System.arraycopy(old, 0, pList, 0, nParents);
				}
				pList[nParents++] = p;
			}
			ptr += 48;
		}
		if (nParents != pList.length) {
			RevCommit[] old = pList;
			pList = new RevCommit[nParents];
			System.arraycopy(old, 0, pList, 0, nParents);
		}
		parents = pList;

		// skip past "author " line
		if (raw[ptr] == 'a')
			while (ptr < rawSize)
				if (raw[ptr++] == '\n')
					break;

		// extract time from "committer "
		commitTime = 0;
		if (raw[ptr] == 'c') {
			while (ptr < rawSize)
				if (raw[ptr++] == '>')
					break;
			ptr++;
			while (ptr < rawSize) {
				final byte b = raw[ptr++];
				if (b < '0' || b > '9')
					break;
				commitTime *= 10;
				commitTime += b - '0';
			}
		}

		buffer = raw;
		flags |= PARSED;
	}

	/**
	 * Time from the "committer " line of the buffer.
	 * 
	 * @return time, expressed as seconds since the epoch.
	 */
	public int getCommitTime() {
		return commitTime;
	}

	/**
	 * Parse this commit buffer for display.
	 * 
	 * @param walk
	 *            revision walker owning this reference.
	 * @return parsed commit.
	 */
	public Commit asCommit(final RevWalk walk) {
		return new Commit(walk.db, this, buffer);
	}

	/**
	 * Get a reference to this commit's tree.
	 * 
	 * @return tree of this commit.
	 */
	public RevTree getTree() {
		return tree;
	}

	/**
	 * Get the number of parent commits listed in this commit.
	 * 
	 * @return number of parents; always a positive value but can be 0.
	 */
	public int getParentCount() {
		return parents.length;
	}

	/**
	 * Get the nth parent from this commit's parent list.
	 * 
	 * @param nth
	 *            parent index to obtain. Must be in the range 0 through
	 *            {@link #getParentCount()}-1.
	 * @return the specified parent.
	 * @throws ArrayIndexOutOfBoundsException
	 *             an invalid parent index was specified.
	 */
	public RevCommit getParent(final int nth) {
		return parents[nth];
	}

	/**
	 * Obtain the raw unparsed commit body (<b>NOTE - THIS IS NOT A COPY</b>).
	 * <p>
	 * This method is exposed only to provide very fast, efficient access to
	 * this commit's message buffer within a RevFilter. Applications relying on
	 * this buffer should be very careful to ensure they do not modify its
	 * contents during their use of it.
	 * 
	 * @return the raw unparsed commit body. This is <b>NOT A COPY</b>.
	 *         Altering the contents of this buffer may alter the walker's
	 *         knowledge of this commit, and the results it produces.
	 */
	public byte[] getRawBuffer() {
		return buffer;
	}

	public void dispose() {
		flags &= ~PARSED;
		buffer = null;
	}
}
