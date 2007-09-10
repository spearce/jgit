package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.spearce.jgit.errors.CheckoutConflictException;

public class WorkDirCheckoutTest extends RepositoryTestCase {
	public void testFindingConflicts() throws IOException {
		GitIndex index = new GitIndex(db);
		index.add(trash, writeTrashFile("bar", "bar"));
		index.add(trash, writeTrashFile("foo/bar/baz/qux", "foo/bar"));
		recursiveDelete(new File(trash, "bar"));
		recursiveDelete(new File(trash, "foo"));
		writeTrashFile("bar/baz/qux/foo", "another nasty one");
		writeTrashFile("foo", "troublesome little bugger");

		WorkDirCheckout workDirCheckout = new WorkDirCheckout(db, trash, index,
				index);
		workDirCheckout.prescanOneTree();
		ArrayList<String> conflictingEntries = workDirCheckout
				.getConflicts();
		ArrayList<String> removedEntries = workDirCheckout.getRemoved();
		assertEquals("bar/baz/qux/foo", conflictingEntries.get(0));
		assertEquals("foo", conflictingEntries.get(1));

		GitIndex index2 = new GitIndex(db);
		recursiveDelete(new File(trash, "bar"));
		recursiveDelete(new File(trash, "foo"));

		index2.add(trash, writeTrashFile("bar/baz/qux/foo", "bar"));
		index2.add(trash, writeTrashFile("foo", "lalala"));

		workDirCheckout = new WorkDirCheckout(db, trash, index2, index);
		workDirCheckout.prescanOneTree();

		conflictingEntries = workDirCheckout.getConflicts();
		removedEntries = workDirCheckout.getRemoved();
		assertTrue(conflictingEntries.isEmpty());
		assertTrue(removedEntries.contains("bar/baz/qux/foo"));
		assertTrue(removedEntries.contains("foo"));
	}

	public void testCheckingOutWithConflicts() throws IOException {
		GitIndex index = new GitIndex(db);
		index.add(trash, writeTrashFile("bar", "bar"));
		index.add(trash, writeTrashFile("foo/bar/baz/qux", "foo/bar"));
		recursiveDelete(new File(trash, "bar"));
		recursiveDelete(new File(trash, "foo"));
		writeTrashFile("bar/baz/qux/foo", "another nasty one");
		writeTrashFile("foo", "troublesome little bugger");

		try {
			WorkDirCheckout workDirCheckout = new WorkDirCheckout(db, trash,
					index, index);
			workDirCheckout.checkout();
			fail("Should have thrown exception");
		} catch (CheckoutConflictException e) {
			// all is well
		}

		WorkDirCheckout workDirCheckout = new WorkDirCheckout(db, trash, index,
				index);
		workDirCheckout.setFailOnConflict(false);
		workDirCheckout.checkout();

		assertTrue(new File(trash, "bar").isFile());
		assertTrue(new File(trash, "foo/bar/baz/qux").isFile());

		GitIndex index2 = new GitIndex(db);
		recursiveDelete(new File(trash, "bar"));
		recursiveDelete(new File(trash, "foo"));
		index2.add(trash, writeTrashFile("bar/baz/qux/foo", "bar"));
		writeTrashFile("bar/baz/qux/bar", "evil? I thought it said WEEVIL!");
		index2.add(trash, writeTrashFile("foo", "lalala"));

		workDirCheckout = new WorkDirCheckout(db, trash, index2, index);
		workDirCheckout.setFailOnConflict(false);
		workDirCheckout.checkout();

		assertTrue(new File(trash, "bar").isFile());
		assertTrue(new File(trash, "foo/bar/baz/qux").isFile());
		assertNotNull(index2.getEntry("bar"));
		assertNotNull(index2.getEntry("foo/bar/baz/qux"));
		assertNull(index2.getEntry("bar/baz/qux/foo"));
		assertNull(index2.getEntry("foo"));
	}
}
