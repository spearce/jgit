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
 * Implementation of IndexTreeVisitor that can be subclassed if you don't
 * case about certain events
 * @author dwatson
 *
 */
public class AbstractIndexTreeVisitor implements IndexTreeVisitor {
	public void finishVisitTree(Tree tree, Tree auxTree, String curDir)
			throws IOException {
		// Empty
	}

	public void finishVisitTree(Tree tree, int i, String curDir)
			throws IOException {
		// Empty
	}

	public void visitEntry(TreeEntry treeEntry, Entry indexEntry, File file)
			throws IOException {
		// Empty
	}

	public void visitEntry(TreeEntry treeEntry, TreeEntry auxEntry,
			Entry indexEntry, File file) throws IOException {
		// Empty
	}
}
