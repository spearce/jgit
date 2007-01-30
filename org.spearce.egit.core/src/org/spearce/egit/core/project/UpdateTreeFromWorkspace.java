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
package org.spearce.egit.core.project;

import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeVisitor;

/**
 * Resynchronize a JGit {@link Tree} with part of the Eclipse workspace.
 * <p>
 * Updates the JGit Tree to more closely match the current status of the Eclipse
 * workspace container it was given.
 * </p>
 * <p>
 * The update rules are quite simple:
 * </p>
 * <ul>
 * <li>If JGit has a subtree, but Eclipse doesn't have that same name as a
 * container, delete the JGit subtree.</li>
 * <li>If JGit has a blob, but Eclipse doesn't have a file by that same name,
 * delete the JGit blob.</li>
 * <li>If both JGit and Eclipse have a blob/tree at the same name, mark the
 * JGit blob modified. This allows a future execution of {@link CheckpointJob}
 * to rehash the file data and determine the correct object name for its
 * contents.</li>
 * <li>Ignore symlinks in the JGit tree.</li>
 * </ul>
 * <p>
 * Note that because every blob/file is marked as modified, the entire JGit Tree
 * will need to be written back out to the Git object database before any sort
 * of meaningful comparsion with another JGit Tree can be performed.
 * </p>
 */
public class UpdateTreeFromWorkspace implements TreeVisitor {
	private IContainer currentContainer;

	/**
	 * Create a new update tree visitor.
	 * 
	 * @param root
	 *            the Eclipse workspace container which corresponds with the
	 *            JGit {@link Tree} object this visitor will be handed to.
	 */
	public UpdateTreeFromWorkspace(final IContainer root) {
		currentContainer = root;
	}

	public void startVisitTree(final Tree t) throws IOException {
		if (t.isRoot()) {
			// JGit visits the root tree, but it has no name. It
			// should corresponding with the IContainer given to
			// us when we were created. So do nothing, and let
			// JGit call us again for each item within the root
			// tree.
			//
			return;
		}

		final IResource r = currentContainer.findMember(t.getName());
		final IContainer c;
		if (r == null)
			c = null;
		else if (r instanceof IContainer)
			c = (IContainer) r;
		else
			c = (IContainer) r.getAdapter(IContainer.class);

		if (c == null || !c.exists())
			t.delete();
		else
			currentContainer = c;
	}

	public void endVisitTree(final Tree t) throws IOException {
		if (t.getParent() != null)
			currentContainer = currentContainer.getParent();
	}

	public void visitFile(final FileTreeEntry f) throws IOException {
		final IResource r = currentContainer.findMember(f.getName());
		if (r == null || !r.exists())
			f.delete();
		else if (!(r instanceof IFile) && r.getAdapter(IFile.class) == null)
			f.delete();
		else
			f.setModified();
	}

	public void visitSymlink(final SymlinkTreeEntry s) throws IOException {
		// TODO: handle symlinks. Only problem is that JGit is indepent of
		// Eclipse
		// and Pure Java does not know what to do about symbolic links.
	}
}
