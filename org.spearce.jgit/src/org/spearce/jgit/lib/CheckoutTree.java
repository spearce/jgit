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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;

public class CheckoutTree extends TreeVisitorWithCurrentDirectory {
	private static final String TYPE_BLOB = Constants.TYPE_BLOB;

	public CheckoutTree(final File root) {
		super(root);
	}

	public void visitFile(final FileTreeEntry fte) throws IOException {
		final File destFile = new File(getCurrentDirectory(), fte.getName());
		final ObjectLoader loader = fte.openReader();
		if (loader == null)
			throw new MissingObjectException(fte.getId(), TYPE_BLOB);
		final byte[] data = loader.getBytes();
		if (!TYPE_BLOB.equals(loader.getType()))
			throw new IncorrectObjectTypeException(fte.getId(), TYPE_BLOB);
		final FileOutputStream fos = new FileOutputStream(destFile);
		try {
			fos.write(data);
		} finally {
			fos.close();
		}
	}

	public void visitSymlink(final SymlinkTreeEntry s) throws IOException {
		// TODO: handle symlinks. Only problem is that JGit is indepent of
		// Eclipse
		// and Pure Java does not know what to do about symbolic links.
	}

	public void startVisitTree(final Tree t) throws IOException {
		super.startVisitTree(t);
		getCurrentDirectory().mkdirs();
	}
}
