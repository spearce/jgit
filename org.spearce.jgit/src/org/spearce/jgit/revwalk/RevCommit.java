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
	private static final String TYPE_COMMIT = Constants.TYPE_COMMIT;

	private static final ObjectId id(final byte[] raw, final int offset) {
		return ObjectId.fromString(raw, offset);
	}

	private RevTree tree;

	RevCommit[] parents;

	int commitTime;

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
		final ObjectLoader ldr = walk.db.openObject(id);
		if (ldr == null)
			throw new MissingObjectException(id, TYPE_COMMIT);
		final byte[] data = ldr.getBytes();
		if (Constants.OBJ_COMMIT != ldr.getType())
			throw new IncorrectObjectTypeException(id, TYPE_COMMIT);
		parseCanonical(walk, data);
	}

	void parseCanonical(final RevWalk walk, final byte[] rawCommitBuffer) {
		tree = walk.lookupTree(id(rawCommitBuffer, 5));

		final int rawSize = rawCommitBuffer.length;
		int ptr = 46;
		RevCommit[] pList = new RevCommit[1];
		int nParents = 0;
		for (;;) {
			if (rawCommitBuffer[ptr] != 'p')
				break;
			final RevCommit p = walk.lookupCommit(id(rawCommitBuffer, ptr + 7));
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
		if (rawCommitBuffer[ptr] == 'a')
			while (ptr < rawSize)
				if (rawCommitBuffer[ptr++] == '\n')
					break;

		// extract time from "committer "
		commitTime = 0;
		if (rawCommitBuffer[ptr] == 'c') {
			while (ptr < rawSize)
				if (rawCommitBuffer[ptr++] == '>')
					break;
			ptr++;
			while (ptr < rawSize) {
				final byte b = rawCommitBuffer[ptr++];
				if (b < '0' || b > '9')
					break;
				commitTime *= 10;
				commitTime += b - '0';
			}
		}

		buffer = rawCommitBuffer;
		flags |= PARSED;
	}

	/**
	 * Time from the "committer " line of the buffer.
	 * 
	 * @return time, expressed as milliseconds since the epoch.
	 */
	public long getCommitTime() {
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
		return new Commit(walk.db, getId(), buffer);
	}

	/**
	 * Get a reference to this commit's tree.
	 * 
	 * @return tree of this commit.
	 */
	public RevTree getTree() {
		return tree;
	}

	public void dispose() {
		flags &= ~PARSED;
		buffer = null;
	}
}
