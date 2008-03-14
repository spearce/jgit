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
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;

/** Iterator over an empty tree (a directory with no files). */
public class EmptyTreeIterator extends AbstractTreeIterator {
	/** Create a new iterator with no parent. */
	public EmptyTreeIterator() {
		// Create a root empty tree.
	}

	EmptyTreeIterator(final AbstractTreeIterator p) {
		super(p);
		pathLen = pathOffset;
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(final Repository repo)
			throws IncorrectObjectTypeException, IOException {
		return new EmptyTreeIterator(this);
	}

	@Override
	public ObjectId getEntryObjectId() {
		return ObjectId.zeroId();
	}

	@Override
	protected byte[] idBuffer() {
		return getEntryObjectId().getBytes();
	}

	@Override
	protected int idOffset() {
		return 0;
	}

	@Override
	public boolean eof() {
		return true;
	}

	@Override
	public void next() throws CorruptObjectException {
		// Do nothing.
	}
}
