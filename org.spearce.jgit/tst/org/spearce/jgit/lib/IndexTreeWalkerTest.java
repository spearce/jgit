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
import java.util.ArrayList;

import org.spearce.jgit.lib.GitIndex.Entry;

public class IndexTreeWalkerTest extends RepositoryTestCase {
	private ArrayList<String> treeOnlyEntriesVisited = new ArrayList<String>();
	private ArrayList<String> bothVisited = new ArrayList<String>();
	private ArrayList<String> indexOnlyEntriesVisited = new ArrayList<String>();
	
	private class TestIndexTreeVisitor extends AbstractIndexTreeVisitor {
		public void visitEntry(TreeEntry treeEntry, Entry indexEntry, File file) {
			if (treeEntry == null)
				indexOnlyEntriesVisited.add(indexEntry.getName());
			else if (indexEntry == null)
				treeOnlyEntriesVisited.add(treeEntry.getFullName());
			else bothVisited.add(indexEntry.getName());
		}
	}

	/*
	 * Need to think about what I really need to be able to do....
	 * 
	 * 1) Visit all entries in index and tree
	 * 2) Get all directories that exist in the index, but not in the tree
	 *    -- I'm pretty sure that I don't need to do the other way around
	 *       because I already 
	 */
	
	public void testTreeOnlyOneLevel() throws IOException {
		GitIndex index = new GitIndex(db);
		Tree tree = new Tree(db);
		tree.addFile("foo");
		tree.addFile("bar");

		new IndexTreeWalker(index, tree, trash, new TestIndexTreeVisitor()).walk();
		
		assertTrue(treeOnlyEntriesVisited.get(0).equals("bar"));
		assertTrue(treeOnlyEntriesVisited.get(1).equals("foo"));
	}
	
	public void testIndexOnlyOneLevel() throws IOException {
		GitIndex index = new GitIndex(db);
		Tree tree = new Tree(db);

		index.add(trash, writeTrashFile("foo", "foo"));
		index.add(trash, writeTrashFile("bar", "bar"));
		new IndexTreeWalker(index, tree, trash, new TestIndexTreeVisitor()).walk();
		
		assertTrue(indexOnlyEntriesVisited.get(0).equals("bar"));
		assertTrue(indexOnlyEntriesVisited.get(1).equals("foo"));
	}
	
	public void testBoth() throws IOException {
		GitIndex index = new GitIndex(db);
		Tree tree = new Tree(db);

		index.add(trash, writeTrashFile("a", "a"));
		tree.addFile("b/b");
		index.add(trash, writeTrashFile("c", "c"));
		tree.addFile("c");
		
		new IndexTreeWalker(index, tree, trash, new TestIndexTreeVisitor()).walk();
		assertTrue(indexOnlyEntriesVisited.contains("a"));
		assertTrue(treeOnlyEntriesVisited.contains("b/b"));
		assertTrue(bothVisited.contains("c"));
		
	}
	
	public void testIndexOnlySubDirs() throws IOException {
		GitIndex index = new GitIndex(db);
		Tree tree = new Tree(db);

		index.add(trash, writeTrashFile("foo/bar/baz", "foobar"));
		index.add(trash, writeTrashFile("asdf", "asdf"));
		new IndexTreeWalker(index, tree, trash, new TestIndexTreeVisitor()).walk();
		
		assertEquals("asdf", indexOnlyEntriesVisited.get(0));
		assertEquals("foo/bar/baz", indexOnlyEntriesVisited.get(1));
	}
	
	public class MyIndexTreeVisitor extends AbstractIndexTreeVisitor {
		public ArrayList<String> visits = new ArrayList<String>();
		public void visitEntry(TreeEntry treeEntry, TreeEntry auxEntry,
				Entry indexEntry, File file) throws IOException {
			visits.add(file.getPath());
		}
	
		public void visitEntry(TreeEntry treeEntry, Entry indexEntry, File file)
				throws IOException {
			visits.add(file.getPath());
		}
	}
	
	public void testLeavingTree() throws IOException {
		GitIndex index = new GitIndex(db);
		index.add(trash, writeTrashFile("foo/bar", "foo/bar"));
		index.add(trash, writeTrashFile("foobar", "foobar"));
		
		new IndexTreeWalker(index, db.mapTree(index.writeTree()), trash, new AbstractIndexTreeVisitor() {
			@Override
			public void visitEntry(TreeEntry entry, Entry indexEntry, File f) {
				if (entry == null || indexEntry == null)
					fail();
			}
			
			@Override
			public void finishVisitTree(Tree tree, int i, String curDir)
					throws IOException {
				if (tree.memberCount() == 0)
					fail();
				if (i == 0)
					fail();
			}
		
		}).walk();
	}
}
