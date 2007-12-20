/*
 *  Copyright (C) 2007 David Watson <dwatson@mimvista.com>
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
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */

package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * Visitor interface for traversing the index and two trees in parallel.
 *
 * When merging we deal with up to two tree nodes and a base node. Then
 * we figure out what to do.
 *
 * A File argument is supplied to allow us to check for modifications in
 * a work tree or update the file.
 */
public interface IndexTreeVisitor {
	/**
	 * Visit a blob, and corresponding tree and index entries.
	 *
	 * @param treeEntry
	 * @param indexEntry
	 * @param file
	 * @throws IOException
	 */
	public void visitEntry(TreeEntry treeEntry, Entry indexEntry, File file) throws IOException;
	
	/**
	 * Visit a blob, and corresponding tree nodes and associated index entry.
	 *
	 * @param treeEntry
	 * @param auxEntry
	 * @param indexEntry
	 * @param file
	 * @throws IOException
	 */
	public void visitEntry(TreeEntry treeEntry, TreeEntry auxEntry, Entry indexEntry, File file) throws IOException;

	/**
	 * Invoked after handling all child nodes of a tree, during a three way merge
	 *
	 * @param tree
	 * @param auxTree
	 * @param i
	 * @param curDir
	 * @throws IOException
	 */
	public void finishVisitTree(Tree tree, Tree auxTree, int i, String curDir) throws IOException;

	/**
	 * Invoked after handling all child nodes of a tree, during two way merge.
	 *
	 * @param tree
	 * @param i
	 * @param curDir
	 * @throws IOException
	 */
	public void finishVisitTree(Tree tree, int i, String curDir) throws IOException;
}
