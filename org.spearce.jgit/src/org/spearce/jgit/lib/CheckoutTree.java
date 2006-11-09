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
import java.io.InputStream;

import org.spearce.jgit.errors.MissingObjectException;

public class CheckoutTree extends TreeVisitorWithCurrentDirectory {
    private final byte[] copyBuffer;

    public CheckoutTree(final File root) {
	super(root);
	copyBuffer = new byte[8192];
    }

    public void visitFile(final FileTreeEntry f) throws IOException {
	final File destFile = new File(getCurrentDirectory(), f.getName());
	final ObjectReader or = f.openReader();

	if (or == null) {
	    throw new MissingObjectException(f.getId(), Constants.TYPE_BLOB);
	}

	try {
	    final InputStream is = or.getInputStream();
	    try {
		final FileOutputStream fos = new FileOutputStream(destFile);
		try {
		    int r;
		    while ((r = is.read(copyBuffer)) > 0) {
			fos.write(copyBuffer, 0, r);
		    }
		} finally {
		    fos.close();
		}
	    } finally {
		or.close();
	    }
	} catch (IOException ioe) {
	    destFile.delete();
	    throw ioe;
	}
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException {
    }

    public void startVisitTree(final Tree t) throws IOException {
	super.startVisitTree(t);
	getCurrentDirectory().mkdirs();
    }
}
