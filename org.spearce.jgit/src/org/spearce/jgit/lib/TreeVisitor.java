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
 * A TreeVisitor is invoked depth first for every node in a tree and is expected
 * to perform different actions.
 */
public interface TreeVisitor {
	/**
	 * Visit to a tree node before child nodes are visited.
	 *
	 * @param t
	 *            Tree
	 * @throws IOException
	 */
	public void startVisitTree(final Tree t) throws IOException;

	/**
	 * Visit to a tree node. after child nodes have been visited.
	 *
	 * @param t Tree
	 * @throws IOException
	 */
	public void endVisitTree(final Tree t) throws IOException;

	/**
	 * Visit to a blob.
	 *
	 * @param f Blob
	 * @throws IOException
	 */
	public void visitFile(final FileTreeEntry f) throws IOException;

	/**
	 * Visit to a symlink.
	 *
	 * @param s Symlink entry
	 * @throws IOException
	 */
	public void visitSymlink(final SymlinkTreeEntry s) throws IOException;
}
