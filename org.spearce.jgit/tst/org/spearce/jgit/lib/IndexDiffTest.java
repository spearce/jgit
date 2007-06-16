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

import java.io.File;
import java.io.IOException;

public class IndexDiffTest extends RepositoryTestCase {
	public void testAdded() throws IOException {
		GitIndex index = new GitIndex(db);
		writeTrashFile("file1", "file1");
		writeTrashFile("dir/subfile", "dir/subfile");
		Tree tree = new Tree(db);

		index.add(trash, new File(trash, "file1"));
		index.add(trash, new File(trash, "dir/subfile"));
		IndexDiff diff = new IndexDiff(tree, index);
		diff.diff();
		assertTrue(diff.getAdded().contains("file1"));
		assertTrue(diff.getAdded().contains("dir/subfile"));
	}

	public void testRemoved() throws IOException {
		GitIndex index = new GitIndex(db);
		writeTrashFile("file2", "file2");
		writeTrashFile("dir/file3", "dir/file3");

		Tree tree = new Tree(db);
		tree.addFile("file2");
		tree.addFile("dir/file3");
		assertEquals(2, tree.memberCount());

		IndexDiff diff = new IndexDiff(tree, index);
		diff.diff();
		assertTrue(diff.getRemoved().contains("file2"));
		assertTrue(diff.getRemoved().contains("dir/file3"));
	}

	public void testModified() throws IOException {
		GitIndex index = new GitIndex(db);
		writeTrashFile("file2", "file2");
		writeTrashFile("dir/file3", "dir/file3");
		index.add(trash, new File(trash, "file2"));
		index.add(trash, new File(trash, "dir/file3"));

		Tree tree = new Tree(db);
		tree.addFile("file2");
		tree.addFile("dir/file3");
		assertEquals(2, tree.memberCount());

		IndexDiff diff = new IndexDiff(tree, index);
		diff.diff();
		assertTrue(diff.getChanged().contains("file2"));
		assertTrue(diff.getChanged().contains("dir/file3"));
	}
}
