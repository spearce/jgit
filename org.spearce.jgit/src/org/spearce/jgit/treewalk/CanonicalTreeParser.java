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
package org.spearce.jgit.treewalk;

import java.io.IOException;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.FileMode;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.Repository;

/** Parses raw Git trees from the canonical semi-text/semi-binary format. */
public class CanonicalTreeParser extends AbstractTreeIterator {
	private byte[] raw;

	private int rawPtr;

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
		rawPtr = 0;
	}

	/**
	 * Reset this parser to walk through the given tree.
	 * 
	 * @param repo
	 *            repository to load the tree data from.
	 * @param id
	 *            identity of the tree being parsed; used only in exception
	 *            messages if data corruption is found.
	 * @throws IncorrectObjectTypeException
	 *             the object supplied as an argument is not actually a tree and
	 *             cannot be parsed as though it were a tree.
	 * @throws IOException
	 *             a loose object or pack file could not be read.
	 */
	public void reset(final Repository repo, final ObjectId id)
			throws IncorrectObjectTypeException, IOException {
		final ObjectLoader ldr = repo.openObject(id);
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
	protected byte[] idBuffer() {
		return raw;
	}

	@Override
	protected int idOffset() {
		return rawPtr - Constants.OBJECT_ID_LENGTH;
	}

	public boolean eof() {
		return raw == null;
	}

	public void next() throws CorruptObjectException {
		int ptr = rawPtr;
		if (ptr >= raw.length) {
			raw = null;
			return;
		}

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
		rawPtr = ptr + Constants.OBJECT_ID_LENGTH;
	}
}
