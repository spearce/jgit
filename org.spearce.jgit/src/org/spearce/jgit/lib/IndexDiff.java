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
import java.util.HashSet;

import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * Compares the Index, a Tree, and the working directory
 */
public class IndexDiff {
	private GitIndex index;
	private Tree tree;
	
	/**
	 * Construct an indexdiff for diffing the workdir against
	 * the index.
	 *
	 * @param repository
	 * @throws IOException
	 */
	public IndexDiff(Repository repository) throws IOException {
		this.tree = repository.mapTree("HEAD");
		this.index = repository.getIndex();
	}

	/**
	 * Construct an indexdiff for diffing the workdir against both
	 * the index and a tree.
	 *
	 * @param tree
	 * @param index
	 */
	public IndexDiff(Tree tree, GitIndex index) {
		this.tree = tree;
		this.index = index;
	}
	
	boolean anyChanges = false;
	
	/**
	 * Run the diff operation. Until this is called, all lists will be empty
	 * @return if anything is different between index, tree, and workdir
	 * @throws IOException
	 */
	public boolean diff() throws IOException {
		final File root = index.getRepository().getDirectory().getParentFile();
		new IndexTreeWalker(index, tree, root, new AbstractIndexTreeVisitor() {
			public void visitEntry(TreeEntry treeEntry, Entry indexEntry, File file) {
				if (treeEntry == null) {
					added.add(indexEntry.getName());
					anyChanges = true;
				} else if (indexEntry == null) {
					removed.add(treeEntry.getFullName());
					anyChanges = true;
				} else {
					if (!treeEntry.getId().equals(indexEntry.getObjectId())) {
						changed.add(indexEntry.getName());
						anyChanges = true;
					}
				}
				
				if (indexEntry != null) {
					if (!file.exists()) {
						missing.add(indexEntry.getName());
						anyChanges = true;
					} else {
						if (indexEntry.isModified(root, true)) {
							modified.add(indexEntry.getName());
							anyChanges = true;
						}
					}
				}
			}
		}).walk();
		
		return anyChanges;
	}

	HashSet<String> added = new HashSet<String>();
	HashSet<String> changed = new HashSet<String>();
	HashSet<String> removed = new HashSet<String>();
	HashSet<String> missing = new HashSet<String>();
	HashSet<String> modified = new HashSet<String>();

	/**
	 * @return list of files added to the index, not in the tree
	 */
	public HashSet<String> getAdded() {
		return added;
	}

	/**
	 * @return list of files changed from tree to index
	 */
	public HashSet<String> getChanged() {
		return changed;
	}

	/**
	 * @return list of files removed from index, but in tree
	 */
	public HashSet<String> getRemoved() {
		return removed;
	}

	/**
	 * @return list of files in index, but not filesystem
	 */
	public HashSet<String> getMissing() {
		return missing;
	}
	
	/**
	 * @return list of files on modified on disk relative to the index
	 */
	public HashSet<String> getModified() {
		return modified;
	}
}
