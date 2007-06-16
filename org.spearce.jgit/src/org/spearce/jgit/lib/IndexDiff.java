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
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */

package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.HashSet;

import org.spearce.jgit.lib.GitIndex.Entry;

public class IndexDiff {
	private GitIndex index;
	private Tree tree;

	public IndexDiff(Tree tree, GitIndex index) {
		this.tree = tree;
		this.index = index;
	}

	public void diff() throws IOException {
		for (Entry entry : index.getMembers()) {
			String filename = entry.getName();
//			if (checked.contains(filename))
//				continue;

			TreeEntry treeBlob = tree.findBlobMember(filename);
			if (treeBlob == null) {
				added.add(filename);
			} else if (entry.getObjectId() != null && !entry.getObjectId().equals(treeBlob.getId())) {
				changed.add(filename);
			}

			checked.add(filename);
		}

		tree.accept(new TreeVisitor() {

			public void visitSymlink(SymlinkTreeEntry s) throws IOException {
			}

			public void visitFile(FileTreeEntry f) throws IOException {
				if (checked.contains(f.getFullName()))
					return;
				removed.add(f.getFullName());
			}

			public void startVisitTree(Tree t) throws IOException {
				// TODO Auto-generated method stub

			}

			public void endVisitTree(Tree t) throws IOException {
				// TODO Auto-generated method stub

			}

		});
	}

	private HashSet<String> checked = new HashSet<String>();
	private HashSet<String> added = new HashSet<String>();
	private HashSet<String> changed = new HashSet<String>();
	private HashSet<String> removed = new HashSet<String>();

	public HashSet<String> getAdded() {
		return added;
	}

	public HashSet<String> getChanged() {
		return changed;
	}

	public HashSet<String> getRemoved() {
		return removed;
	}
}
