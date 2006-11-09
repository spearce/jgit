/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.IOException;

public class FileTreeEntry extends TreeEntry {
    private FileMode mode;

    public FileTreeEntry(final Tree parent, final ObjectId id,
	    final byte[] nameUTF8, final boolean execute) {
	super(parent, id, nameUTF8);
	setExecutable(execute);
    }

    public FileMode getMode() {
	return mode;
    }

    public boolean isExecutable() {
	return getMode().equals(FileMode.EXECUTABLE_FILE);
    }

    public void setExecutable(final boolean execute) {
	mode = execute ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
    }

    public ObjectReader openReader() throws IOException {
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
