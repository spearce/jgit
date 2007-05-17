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

public class ForceModified implements TreeVisitor {
	public void startVisitTree(final Tree t) throws IOException {
		t.setModified();
	}

	public void endVisitTree(final Tree t) throws IOException {
		// Nothing to do.
	}

	public void visitFile(final FileTreeEntry f) throws IOException {
		f.setModified();
	}

	public void visitSymlink(final SymlinkTreeEntry s) throws IOException {
		// TODO: handle symlinks. Only problem is that JGit is indepent of
		// Eclipse
		// and Pure Java does not know what to do about symbolic links.
	}
}
