/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.treewalk;

import java.io.IOException;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Repository;

/** Parses raw Git trees from the canonical semi-text/semi-binary format. */
public class CanonicalTreeParser extends AbstractTreeIterator {
	private byte[] raw;

	/** First offset within {@link #raw} of the current entry's data. */
	private int currPtr;

	/** Offset one past the current entry (first byte of next entry. */
	private int nextPtr;

	/** Create a new parser. */
	public CanonicalTreeParser() {
		// Nothing necessary.
	}

	private CanonicalTreeParser(final CanonicalTreeParser p) {
		super(p);
	}

	/**
	 * Reset this parser to walk through the given tree data.
	 * 
	 * @param treeData
	 *            the raw tree content.
	 */
	public void reset(final byte[] treeData) {
		raw = treeData;
		currPtr = 0;
		if (!eof())
			parseEntry();
	}

	/**
	 * Reset this parser to walk through the given tree.
	 * 
	 * @param repo
	 *            repository to load the tree data from.
	 * @param id
	 *            identity of the tree being parsed; used only in exception
	 *            messages if data corruption is found.
	 * @throws MissingObjectException
	 *             the object supplied is not available from the repository.
	 * @throws IncorrectObjectTypeException
	 *             the object supplied as an argument is not actually a tree and
	 *             cannot be parsed as though it were a tree.
	 * @throws IOException
	 *             a loose object or pack file could not be read.
	 */
	public void reset(final Repository repo, final ObjectId id)
			throws IncorrectObjectTypeException, IOException {
		final ObjectLoader ldr = repo.openObject(id);
		if (ldr == null)
			throw new MissingObjectException(id, Constants.TYPE_TREE);
		final byte[] subtreeData = ldr.getCachedBytes();
		if (ldr.getType() != Constants.OBJ_TREE)
			throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
		reset(subtreeData);
	}

	public CanonicalTreeParser createSubtreeIterator(final Repository repo)
			throws IncorrectObjectTypeException, IOException {
		final ObjectId id = getEntryObjectId();
		if (!FileMode.TREE.equals(mode))
			throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
		final CanonicalTreeParser p = new CanonicalTreeParser(this);
		p.reset(repo, id);
		return p;
	}

	@Override
	public byte[] idBuffer() {
		return raw;
	}

	@Override
	public int idOffset() {
		return nextPtr - Constants.OBJECT_ID_LENGTH;
	}

	@Override
	public boolean first() {
		return currPtr == 0;
	}

	public boolean eof() {
		return currPtr == raw.length;
	}

	@Override
	public void next(int delta) {
		if (delta == 1) {
			// Moving forward one is the most common case.
			//
			currPtr = nextPtr;
			if (!eof())
				parseEntry();
			return;
		}

		// Fast skip over records, then parse the last one.
		//
		final int end = raw.length;
		int ptr = nextPtr;
		while (--delta > 0 && ptr != end) {
			while (raw[ptr] != 0)
				ptr++;
			ptr += Constants.OBJECT_ID_LENGTH + 1;
		}
		if (delta != 0)
			throw new ArrayIndexOutOfBoundsException(delta);
		currPtr = ptr;
		if (!eof())
			parseEntry();
	}

	@Override
	public void back(int delta) {
		int ptr = currPtr;
		while (--delta >= 0) {
			if (ptr == 0)
				throw new ArrayIndexOutOfBoundsException(delta);

			// Rewind back beyond the id and the null byte. Find the
			// last space, this _might_ be the split between the mode
			// and the path. Most paths in most trees do not contain a
			// space so this prunes our search more quickly.
			//
			ptr -= Constants.OBJECT_ID_LENGTH;
			while (raw[--ptr] != ' ')
				/* nothing */;
			if (--ptr < Constants.OBJECT_ID_LENGTH) {
				if (delta != 0)
					throw new ArrayIndexOutOfBoundsException(delta);
				ptr = 0;
				break;
			}

			// Locate a position that matches "\0.{20}[0-7]" such that
			// the ptr will rest on the [0-7]. This must be the first
			// byte of the mode. This search works because the path in
			// the prior record must have a non-zero length and must not
			// contain a null byte.
			//
			for (int n;; ptr = n) {
				n = ptr - 1;
				final byte b = raw[n];
				if ('0' <= b && b <= '7')
					continue;
				if (raw[n - Constants.OBJECT_ID_LENGTH] != 0)
					continue;
				break;
			}
		}
		currPtr = ptr;
		parseEntry();
	}

	private void parseEntry() {
		int ptr = currPtr;
		byte c = raw[ptr++];
		int tmp = c - '0';
		for (;;) {
			c = raw[ptr++];
			if (' ' == c)
				break;
			tmp <<= 3;
			tmp += c - '0';
		}
		mode = tmp;

		tmp = pathOffset;
		for (;; tmp++) {
			c = raw[ptr++];
			if (c == 0)
				break;
			try {
				path[tmp] = c;
			} catch (ArrayIndexOutOfBoundsException e) {
				growPath(tmp);
				path[tmp] = c;
			}
		}
		pathLen = tmp;
		nextPtr = ptr + Constants.OBJECT_ID_LENGTH;
	}
}
