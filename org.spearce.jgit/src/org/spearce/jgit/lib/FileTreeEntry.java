/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.lib;

import java.io.IOException;

/**
 * A representation of a file (blob) object in a {@link Tree}.
 */
public class FileTreeEntry extends TreeEntry {
	private FileMode mode;

	/**
	 * Constructor for a File (blob) object.
	 *
	 * @param parent
	 *            The {@link Tree} holding this object (or null)
	 * @param id
	 *            the SHA-1 of the blob (or null for a yet unhashed file)
	 * @param nameUTF8
	 *            raw object name in the parent tree
	 * @param execute
	 *            true if the executable flag is set
	 */
	public FileTreeEntry(final Tree parent, final ObjectId id,
			final byte[] nameUTF8, final boolean execute) {
		super(parent, id, nameUTF8);
		setExecutable(execute);
	}

	public FileMode getMode() {
		return mode;
	}

	/**
	 * @return true if this file is executable
	 */
	public boolean isExecutable() {
		return getMode().equals(FileMode.EXECUTABLE_FILE);
	}

	/**
	 * @param execute set/reset the executable flag
	 */
	public void setExecutable(final boolean execute) {
		mode = execute ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
	}

	/**
	 * @return an {@link ObjectLoader} that will return the data
	 * @throws IOException
	 */
	public ObjectLoader openReader() throws IOException {
		return getRepository().openBlob(getId());
	}

	public void accept(final TreeVisitor tv, final int flags)
			throws IOException {
		if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified()) {
			return;
		}

		tv.visitFile(this);
	}

	public String toString() {
		final StringBuffer r = new StringBuffer();
		r.append(ObjectId.toString(getId()));
		r.append(' ');
		r.append(isExecutable() ? 'X' : 'F');
		r.append(' ');
		r.append(getFullName());
		return r.toString();
	}
}
