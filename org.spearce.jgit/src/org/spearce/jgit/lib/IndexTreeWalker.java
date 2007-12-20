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
 * A class for traversing the index and one or two trees.
 *
 * A visitor is invoked for executing actions, like figuring out how to merge.
 */
public class IndexTreeWalker {
	private final Tree mainTree;
	private final Tree newTree;
	private final File root;
	private final IndexTreeVisitor visitor;
	private boolean threeTrees;

	/**
	 * Construct a walker for the index and one tree.
	 *
	 * @param index
	 * @param tree
	 * @param root
	 * @param visitor
	 */
	public IndexTreeWalker(GitIndex index, Tree tree, File root, IndexTreeVisitor visitor) {
		this.mainTree = tree;
		this.root = root;
		this.visitor = visitor;
		this.newTree = null;
		
		threeTrees = false;
		
		indexMembers = index.getMembers();
	}
	
	/**
	 * Construct a walker for the index and two trees.
	 *
	 * @param index
	 * @param mainTree
	 * @param newTree
	 * @param root
	 * @param visitor
	 */
	public IndexTreeWalker(GitIndex index, Tree mainTree, Tree newTree, File root, IndexTreeVisitor visitor) {
		this.mainTree = mainTree;
		this.newTree = newTree;
		this.root = root;
		this.visitor = visitor;
		
		threeTrees = true;
		
		indexMembers = index.getMembers();
	}

	Entry[] indexMembers;
	int indexCounter = 0;
	
	/**
	 * Actually walk the index tree
	 *
	 * @throws IOException
	 */
	public void walk() throws IOException {
		walk(mainTree, newTree, "/");
	}

	private void walk(Tree tree, Tree auxTree, String curDir) throws IOException {
		TreeEntry[] treeMembers = tree == null ? new TreeEntry[0] : tree.members();
		TreeEntry[] auxTreeMembers = auxTree == null ? new TreeEntry[0] : auxTree.members();
		int treeCounter = 0;
		int auxTreeCounter = 0;
		
		int curIndexPos = indexCounter;
		
		while (treeCounter < treeMembers.length || 
				indexCounter < indexMembers.length || 
				auxTreeCounter < auxTreeMembers.length) {
			TreeEntry h = treeCounter < treeMembers.length ? treeMembers[treeCounter] : null;
			TreeEntry m = auxTreeCounter < auxTreeMembers.length ? auxTreeMembers[auxTreeCounter] : null;
			Entry i = indexCounter < indexMembers.length ? indexMembers[indexCounter] : null;

			String indexName = i == null ? null : i.getName();
			String treeName = h == null ? null : h.getFullName();
			String auxTreeName = m == null ? null : m.getFullName();
			
			if (treeName != null && indexName != null && auxTreeName != null) { 
				if (eq(h, i) && eq(m, i)) {
					// H == I == M
					visitor.visitEntry(h, m, i, new File(root, treeName));
					
					treeCounter++;
					indexCounter++;
					auxTreeCounter++;
				} else if (eq(h, i) && lt(h, m)) {
					// H == I, H < M
					visitor.visitEntry(h, null, i, new File(root, treeName));
					
					treeCounter++;
					indexCounter++;
				} else if (eq(h, m) && lt(h, i)) {
					// H == M, H < I
					if (h instanceof Tree) {
						walk((Tree)h, (Tree)m, "/" + treeName);
					} else {
						visitor.visitEntry(h, m, null, new File(root, treeName));
					}
					
					treeCounter++;
					auxTreeCounter++;
					
					
				} else if (eq(m, i) && lt(i, h)) {
					// I == M, I < H
					visitor.visitEntry(null, m, i, new File(root, indexName));
					
					indexCounter++;
					auxTreeCounter++;
				} else if (lt(h, i) && lt(h, m)) {
					// H < I, H < M
					if (h instanceof Tree) {
						walk((Tree) h, null, "/" + treeName);
					} else {
						visitor.visitEntry(h, null, null, new File(root, treeName));
					}
					
					treeCounter++;
				} else if (lt(m, i) && lt(m, h)) {
					// M < I, M < H
					if (m instanceof Tree) {
						walk(null, (Tree) m, "/" + auxTreeName);
					} else {
						visitor.visitEntry(null, m, null, new File(root, auxTreeName));
					}
					
					auxTreeCounter++;
				} else { // index entry is first
					// I < H, I < M
					visitor.visitEntry(null, null, i, new File(root, indexName));
					
					indexCounter++;
				}
			} else if (treeName != null && indexName != null) {
				if (eq(h, i)) {
					if (threeTrees)
						visitor.visitEntry(h, null, i, new File(root, indexName));
					else visitor.visitEntry(h, i, new File(root, indexName));
					
					treeCounter++;
					indexCounter++;
				} else if (lt(h, i)) {
					if (h instanceof Tree) 
						walk((Tree) h, null, "/" + treeName);
					else {
						if (threeTrees) {
							visitor.visitEntry(h, null, null, new File(root, treeName));
						} else visitor.visitEntry(h, null, new File(root, treeName));
					}
					
					treeCounter++;
				} else { // lt(i, h)
					if (threeTrees) {
						visitor.visitEntry(null, null, i, new File(root, indexName));
					} else visitor.visitEntry(null, i, new File(root, indexName));
					
					indexCounter++;
				}
			} else if (treeName != null && auxTreeName != null) {
				if (eq(h, m)) {
					if (h instanceof Tree) {
						walk((Tree) h, (Tree) m, "/" + treeName);
					} else {
						visitor.visitEntry(h, m, null, new File(root, treeName));
					}
					
					treeCounter++;
					auxTreeCounter++;
				} else if (lt(h, m)) {
					if (h instanceof Tree) {
						walk((Tree) h, null, "/" + treeName);
					} else {
						visitor.visitEntry(h, null, null, new File(root, treeName));
					}
					
					treeCounter++;
				} else { // lt(m, h)
					if (m instanceof Tree) {
						walk(null, (Tree) m, "/" + auxTreeName);
					} else {
						visitor.visitEntry(null, m, null, new File("/" + auxTreeName));
					}
						
					auxTreeCounter++;
				}
				
			} else if (indexName != null && auxTreeName != null) {
				if (eq(m, i)) {
					visitor.visitEntry(null, m, i, new File(root, indexName));
					
					auxTreeCounter++;
					indexCounter++;
				} else if (lt(m, i)) {
					if (m instanceof Tree) 
						walk(null, (Tree) m, "/" + auxTreeName);
					else {
						visitor.visitEntry(null, m, null, new File(root, auxTreeName));
					}
					
					auxTreeCounter++;
				} else { // lt(i, m)
					visitor.visitEntry(null, null, i, new File(root, indexName));
					
					indexCounter++;
				}
			} else if (treeName != null) {
				if (h instanceof Tree) {
					walk((Tree) h, null, "/" + treeName);
				} else if (threeTrees)
					visitor.visitEntry(h, null, null, new File(root, treeName));
				else visitor.visitEntry(h, null, new File(root, treeName));
				
				treeCounter++;
			} else if (auxTreeName != null) {
				if (m instanceof Tree) {
					walk(null, (Tree)m, "/" + auxTreeName);
				} else visitor.visitEntry(null, m, null, new File(root, auxTreeName));
				
				auxTreeCounter++;
			} else if (indexName != null) {
				// need to check if we're done with this tree
				String curDirNoSlash = curDir.substring(1);
				if (!indexName.startsWith(curDirNoSlash + "/") && curDirNoSlash.length() != 0)
					break;
				
				if (threeTrees)
					visitor.visitEntry(null, null, i, new File(root, indexName));
				else visitor.visitEntry(null, i, new File(root, indexName));
				indexCounter++;
			}
		}
		
		if (threeTrees) {
			visitor.finishVisitTree(tree, auxTree, indexCounter - curIndexPos, 
					curDir.substring(1));
		} else {
			visitor.finishVisitTree(tree, indexCounter - curIndexPos, curDir);
		}
	}

	static boolean lt(TreeEntry h, Entry i) {
		return compare(h, i) < 0;
	}
	
	static boolean lt(Entry i, TreeEntry t) {
		return compare(t, i) > 0;
	}

	static boolean lt(TreeEntry h, TreeEntry m) {
		return compare(h, m) < 0;
	}
	
	static boolean eq(TreeEntry t1, TreeEntry t2) {
		return compare(t1, t2) == 0;
	}
	
	static boolean eq(TreeEntry t1, Entry e) {
		return compare(t1, e) == 0;
	}

	static int compare(TreeEntry t, Entry i) {
		if (t.getFullName().equals(i.getName())) {
			if (t instanceof Tree)
				return 1;
			return 0;
		}
		return t.getFullName().compareTo(i.getName());
	}
	
	static int compare(TreeEntry t1, TreeEntry t2) {
		if (t1.getName().equals(t2.getName())) {
			if (t1 instanceof Tree && t2 instanceof Tree)
				return 0;
			if (t1 instanceof Tree)
				return 1;
			if (t2 instanceof Tree)
				return -1;
			return 0;
		}
		return t1.getName().compareTo(t2.getName());
	}
	
	static int compare(byte[] name1, byte[] name2) {
		for (int i = 0; i < name1.length && i < name2.length; i++) {
			if (name1[i] < name2[i])
				return -1;
			if (name1[i] > name2[i])
				return 1;
		}
		if (name1.length < name2.length)
			return -1;
		if (name2.length < name1.length)
			return 1;
		return 0;
	}
}
