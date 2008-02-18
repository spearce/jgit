package org.spearce.jgit.lib;

import java.io.IOException;

public class TreeIteratorPreOrderTest extends RepositoryTestCase {

	/** Empty tree */
	public void testEmpty() {
		Tree tree = new Tree(db);
		TreeIterator i = makeIterator(tree);
		assertTrue(i.hasNext());
		assertEquals("", i.next().getFullName());
		assertFalse(i.hasNext());
	}

	/**
	 * one file
	 * 
	 * @throws IOException
	 */
	public void testSimpleF1() throws IOException {
		Tree tree = new Tree(db);
		tree.addFile("x");
		TreeIterator i = makeIterator(tree);
		assertTrue(i.hasNext());
		assertEquals("", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("x", i.next().getName());
		assertFalse(i.hasNext());
	}

	/**
	 * two files
	 * 
	 * @throws IOException
	 */
	public void testSimpleF2() throws IOException {
		Tree tree = new Tree(db);
		tree.addFile("a");
		tree.addFile("x");
		TreeIterator i = makeIterator(tree);
		assertTrue(i.hasNext());
		assertEquals("", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a", i.next().getName());
		assertEquals("x", i.next().getName());
		assertFalse(i.hasNext());
	}

	/**
	 * Empty tree
	 * 
	 * @throws IOException
	 */
	public void testSimpleT() throws IOException {
		Tree tree = new Tree(db);
		tree.addTree("a");
		TreeIterator i = makeIterator(tree);
		assertTrue(i.hasNext());
		assertEquals("", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a", i.next().getFullName());
		assertFalse(i.hasNext());
	}
	
	public void testTricky() throws IOException {
		Tree tree = new Tree(db);
		tree.addFile("a.b");
		tree.addFile("a.c");
		tree.addFile("a/b.b/b");
		tree.addFile("a/b");
		tree.addFile("a/c");
		tree.addFile("a=c");
		tree.addFile("a=d");

		TreeIterator i = makeIterator(tree);
		assertTrue(i.hasNext());
		assertEquals("", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a.b", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a.c", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a/b", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a/b.b", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a/b.b/b", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a/c", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a=c", i.next().getFullName());
		assertTrue(i.hasNext());
		assertEquals("a=d", i.next().getFullName());
		assertFalse(i.hasNext());
	}

	private TreeIterator makeIterator(Tree tree) {
		return new TreeIterator(tree, TreeIterator.Order.PREORDER);
	}
}
