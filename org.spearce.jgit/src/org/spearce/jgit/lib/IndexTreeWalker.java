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
		walk(mainTree, newTree, "");
	}

	private void walk(Tree tree, Tree auxTree, String curDir) throws IOException {
		TreeIterator mi = new TreeIterator(tree, TreeIterator.Order.POSTORDER);
		TreeIterator ai = new TreeIterator(auxTree, TreeIterator.Order.POSTORDER);
		TreeEntry m = mi.hasNext() ? mi.next() : null;
		TreeEntry a = ai.hasNext() ? ai.next() : null;
		int curIndexPos = indexCounter;
		Entry i = indexCounter < indexMembers.length ? indexMembers[indexCounter++] : null;
		while (m != null || a != null || i != null) {
			int cmpma = compare(m, a);
			int cmpmi = compare(m, i);
			int cmpai = compare(a, i);
			
			TreeEntry pm = cmpma <= 0 && cmpmi <= 0 ? m : null;
			TreeEntry pa = cmpma >= 0 && cmpai <= 0 ? a : null;
			Entry     pi = cmpmi >= 0 && cmpai >= 0 ? i : null;

			if (pi != null)
				visitEntry(pm, pa, pi, root);
			else
				finishVisitTree(pm, pa, curIndexPos, root);

			if (pm != null) m = mi.hasNext() ? mi.next() : null;
			if (pa != null) a = ai.hasNext() ? ai.next() : null;
			if (pi != null) i = indexCounter < indexMembers.length ? indexMembers[indexCounter++] : null;
		}
	}

	private void visitEntry(TreeEntry t1, TreeEntry t2,
			Entry i, File root) throws IOException {

		assert t1 != null || t2 != null || i != null : "Needs at least one entry";
		assert root != null : "Needs workdir";

		if (t1 != null && t1.getParent() == null)
			t1 = null;
		if (t2 != null && t2.getParent() == null)
			t2 = null;

		File f = null;
		if (i != null)
			f = new File(root, i.getName());
		else if (t1 != null)
			f = new File(root, t1.getFullName());
		else if (t2 != null)
			f = new File(root, t2.getFullName());

		if (t1 != null || t2 != null || i != null)
			if (threeTrees)
				visitor.visitEntry(t1, t2, i, f);
			else
				visitor.visitEntry(t1, i, f);
	}

	private void finishVisitTree(TreeEntry t1, TreeEntry t2, int curIndexPos, File root)
			throws IOException {

		assert t1 != null || t2 != null : "Needs at least one entry";
		assert root != null : "Needs workdir";

		if (t1 != null && t1.getParent() == null)
			t1 = null;
		if (t2 != null && t2.getParent() == null)
			t2 = null;

		File f = null;
		String c= null;
		if (t1 != null) {
			c = t1.getFullName();
			f = new File(root, c);
		} else if (t2 != null) {
			c = t2.getFullName();
			f = new File(root, c);
		}
		if (t1 instanceof Tree || t2 instanceof Tree)
			if (threeTrees)
				visitor.finishVisitTree((Tree)t1, (Tree)t2, c);
			else
				visitor.finishVisitTree((Tree)t1, indexCounter - curIndexPos, c);
		else if (t1 != null || t2 != null)
			if (threeTrees)
				visitor.visitEntry(t1, t2, null, f);
			else
				visitor.visitEntry(t1, null, f);
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
		if (t == null && i == null)
			return 0;
		if (t == null)
			return 1;
		if (i == null)
			return -1;
		return Tree.compareNames(t.getFullNameUTF8(), i.getNameUTF8(), TreeEntry.lastChar(t), TreeEntry.lastChar(i)); 
	}
	
	static int compare(TreeEntry t1, TreeEntry t2) {
		if (t1 != null && t1.getParent() == null && t2 != null && t2.getParent() == null)
			return 0;
		if (t1 != null && t1.getParent() == null)
			return -1;
		if (t2 != null && t2.getParent() == null)
			return 1;

		if (t1 == null && t2 == null)
			return 0;
		if (t1 == null)
			return 1;
		if (t2 == null)
			return -1;
		return Tree.compareNames(t1.getFullNameUTF8(), t2.getFullNameUTF8(), TreeEntry.lastChar(t1), TreeEntry.lastChar(t2));
	}
	
}
