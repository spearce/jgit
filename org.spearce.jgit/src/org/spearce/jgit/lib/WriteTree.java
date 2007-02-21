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

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.errors.SymlinksNotSupportedException;

public class WriteTree extends TreeVisitorWithCurrentDirectory {
	private final ObjectWriter ow;

	public WriteTree(final File sourceDirectory, final Repository db) {
		super(sourceDirectory);
		ow = new ObjectWriter(db);
	}

	public void visitFile(final FileTreeEntry f) throws IOException {
		f.setId(ow.writeBlob(new File(getCurrentDirectory(), f.getName())));
	}

	public void visitSymlink(final SymlinkTreeEntry s) throws IOException {
		if (s.isModified()) {
			throw new SymlinksNotSupportedException("Symlink \""
					+ s.getFullName()
					+ "\" cannot be written as the link target"
					+ " cannot be read from within Java.");
		}
	}

	public void endVisitTree(final Tree t) throws IOException {
		super.endVisitTree(t);
		t.setId(ow.writeTree(t));
	}
}
