/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class T0002_Tree extends RepositoryTestCase {
	private static final ObjectId SOME_FAKE_ID = ObjectId.fromString(
			"0123456789abcdef0123456789abcdef01234567");

	private int compareNamesUsingSpecialCompare(String a,String b) throws UnsupportedEncodingException {
		char lasta = '\0';
		byte[] abytes;
		if (a.length() > 0 && a.charAt(a.length()-1) == '/') {
			lasta = '/';
			a = a.substring(0, a.length() - 1);
		}
		abytes = a.getBytes("ISO-8859-1");
		char lastb = '\0';
		byte[] bbytes;
		if (b.length() > 0 && b.charAt(b.length()-1) == '/') {
			lastb = '/';
			b = b.substring(0, b.length() - 1);
		}
		bbytes = b.getBytes("ISO-8859-1");
		return Tree.compareNames(abytes, bbytes, lasta, lastb);
	}

	public void test000_sort_01() throws UnsupportedEncodingException {
		assertEquals(0, compareNamesUsingSpecialCompare("a","a"));
	}
	public void test000_sort_02() throws UnsupportedEncodingException {
		assertEquals(-1, compareNamesUsingSpecialCompare("a","b"));
		assertEquals(1, compareNamesUsingSpecialCompare("b","a"));
	}
	public void test000_sort_03() throws UnsupportedEncodingException {
		assertEquals(1, compareNamesUsingSpecialCompare("a:","a"));
		assertEquals(1, compareNamesUsingSpecialCompare("a/","a"));
		assertEquals(-1, compareNamesUsingSpecialCompare("a","a/"));
		assertEquals(-1, compareNamesUsingSpecialCompare("a","a:"));
		assertEquals(1, compareNamesUsingSpecialCompare("a:","a/"));
		assertEquals(-1, compareNamesUsingSpecialCompare("a/","a:"));
	}
	public void test000_sort_04() throws UnsupportedEncodingException {
		assertEquals(-1, compareNamesUsingSpecialCompare("a.a","a/a"));
		assertEquals(1, compareNamesUsingSpecialCompare("a/a","a.a"));
	}
	public void test000_sort_05() throws UnsupportedEncodingException {
		assertEquals(-1, compareNamesUsingSpecialCompare("a.","a/"));
		assertEquals(1, compareNamesUsingSpecialCompare("a/","a."));

	}

	public void test001_createEmpty() throws IOException {
		final Tree t = new Tree(db);
		assertTrue("isLoaded", t.isLoaded());
		assertTrue("isModified", t.isModified());
		assertTrue("no parent", t.getParent() == null);
		assertTrue("isRoot", t.isRoot());
		assertTrue("no name", t.getName() == null);
		assertTrue("no nameUTF8", t.getNameUTF8() == null);
		assertTrue("has entries array", t.members() != null);
		assertTrue("entries is empty", t.members().length == 0);
		assertEquals("full name is empty", "", t.getFullName());
		assertTrue("no id", t.getId() == null);
		assertTrue("tree is self", t.getTree() == t);
		assertTrue("database is r", t.getRepository() == db);
		assertTrue("no foo child", t.findTreeMember("foo") == null);
		assertTrue("no foo child", t.findBlobMember("foo") == null);
	}

	public void test002_addFile() throws IOException {
		final Tree t = new Tree(db);
		t.setId(SOME_FAKE_ID);
		assertTrue("has id", t.getId() != null);
		assertFalse("not modified", t.isModified());

		final String n = "bob";
		final FileTreeEntry f = t.addFile(n);
		assertNotNull("have file", f);
		assertEquals("name matches", n, f.getName());
		assertEquals("name matches", f.getName(), new String(f.getNameUTF8(),
				"UTF-8"));
		assertEquals("full name matches", n, f.getFullName());
		assertTrue("no id", f.getId() == null);
		assertTrue("is modified", t.isModified());
		assertTrue("has no id", t.getId() == null);
		assertTrue("found bob", t.findBlobMember(f.getName()) == f);

		final TreeEntry[] i = t.members();
		assertNotNull("members array not null", i);
		assertTrue("iterator is not empty", i != null && i.length > 0);
		assertTrue("iterator returns file", i != null && i[0] == f);
		assertTrue("iterator is empty", i != null && i.length == 1);
	}

	public void test004_addTree() throws IOException {
		final Tree t = new Tree(db);
		t.setId(SOME_FAKE_ID);
		assertTrue("has id", t.getId() != null);
		assertFalse("not modified", t.isModified());

		final String n = "bob";
		final Tree f = t.addTree(n);
		assertNotNull("have tree", f);
		assertEquals("name matches", n, f.getName());
		assertEquals("name matches", f.getName(), new String(f.getNameUTF8(),
				"UTF-8"));
		assertEquals("full name matches", n, f.getFullName());
		assertTrue("no id", f.getId() == null);
		assertTrue("parent matches", f.getParent() == t);
		assertTrue("repository matches", f.getRepository() == db);
		assertTrue("isLoaded", f.isLoaded());
		assertFalse("has items", f.members().length > 0);
		assertFalse("is root", f.isRoot());
		assertTrue("tree is self", f.getTree() == f);
		assertTrue("parent is modified", t.isModified());
		assertTrue("parent has no id", t.getId() == null);
		assertTrue("found bob child", t.findTreeMember(f.getName()) == f);

		final TreeEntry[] i = t.members();
		assertTrue("iterator is not empty", i.length > 0);
		assertTrue("iterator returns file", i[0] == f);
		assertTrue("iterator is empty", i.length == 1);
	}

	public void test005_addRecursiveFile() throws IOException {
		final Tree t = new Tree(db);
		final FileTreeEntry f = t.addFile("a/b/c");
		assertNotNull("created f", f);
		assertEquals("c", f.getName());
		assertEquals("b", f.getParent().getName());
		assertEquals("a", f.getParent().getParent().getName());
		assertTrue("t is great-grandparent", t == f.getParent().getParent()
				.getParent());
	}

	public void test005_addRecursiveTree() throws IOException {
		final Tree t = new Tree(db);
		final Tree f = t.addTree("a/b/c");
		assertNotNull("created f", f);
		assertEquals("c", f.getName());
		assertEquals("b", f.getParent().getName());
		assertEquals("a", f.getParent().getParent().getName());
		assertTrue("t is great-grandparent", t == f.getParent().getParent()
				.getParent());
	}

	public void test006_addDeepTree() throws IOException {
		final Tree t = new Tree(db);

		final Tree e = t.addTree("e");
		assertNotNull("have e", e);
		assertTrue("e.parent == t", e.getParent() == t);
		final Tree f = t.addTree("f");
		assertNotNull("have f", f);
		assertTrue("f.parent == t", f.getParent() == t);
		final Tree g = f.addTree("g");
		assertNotNull("have g", g);
		assertTrue("g.parent == f", g.getParent() == f);
		final Tree h = g.addTree("h");
		assertNotNull("have h", h);
		assertTrue("h.parent = g", h.getParent() == g);

		h.setId(SOME_FAKE_ID);
		assertTrue("h not modified", !h.isModified());
		g.setId(SOME_FAKE_ID);
		assertTrue("g not modified", !g.isModified());
		f.setId(SOME_FAKE_ID);
		assertTrue("f not modified", !f.isModified());
		e.setId(SOME_FAKE_ID);
		assertTrue("e not modified", !e.isModified());
		t.setId(SOME_FAKE_ID);
		assertTrue("t not modified.", !t.isModified());

		assertEquals("full path of h ok", "f/g/h", h.getFullName());
		assertTrue("Can find h", t.findTreeMember(h.getFullName()) == h);
		assertTrue("Can't find f/z", t.findBlobMember("f/z") == null);
		assertTrue("Can't find y/z", t.findBlobMember("y/z") == null);

		final FileTreeEntry i = h.addFile("i");
		assertNotNull(i);
		assertEquals("full path of i ok", "f/g/h/i", i.getFullName());
		assertTrue("Can find i", t.findBlobMember(i.getFullName()) == i);
		assertTrue("h modified", h.isModified());
		assertTrue("g modified", g.isModified());
		assertTrue("f modified", f.isModified());
		assertTrue("e not modified", !e.isModified());
		assertTrue("t modified", t.isModified());

		assertTrue("h no id", h.getId() == null);
		assertTrue("g no id", g.getId() == null);
		assertTrue("f no id", f.getId() == null);
		assertTrue("e has id", e.getId() != null);
		assertTrue("t no id", t.getId() == null);
	}

	public void test007_manyFileLookup() throws IOException {
		final Tree t = new Tree(db);
		final List files = new ArrayList(26 * 26);
		for (char level1 = 'a'; level1 <= 'z'; level1++) {
			for (char level2 = 'a'; level2 <= 'z'; level2++) {
				final String n = "." + level1 + level2 + "9";
				final FileTreeEntry f = t.addFile(n);
				assertNotNull("File " + n + " added.", f);
				assertEquals(n, f.getName());
				files.add(f);
			}
		}
		assertEquals(files.size(), t.memberCount());
		final TreeEntry[] ents = t.members();
		assertNotNull(ents);
		assertEquals(files.size(), ents.length);
		for (int k = 0; k < ents.length; k++) {
			assertTrue("File " + ((FileTreeEntry) files.get(k)).getName()
					+ " is at " + k + ".", files.get(k) == ents[k]);
		}
	}

	public void test008_SubtreeInternalSorting() throws IOException {
		final Tree t = new Tree(db);
		final FileTreeEntry e0 = t.addFile("a-b");
		final FileTreeEntry e1 = t.addFile("a-");
		final FileTreeEntry e2 = t.addFile("a=b");
		final Tree e3 = t.addTree("a");
		final FileTreeEntry e4 = t.addFile("a=");

		final TreeEntry[] ents = t.members();
		assertSame(e1, ents[0]);
		assertSame(e0, ents[1]);
		assertSame(e3, ents[2]);
		assertSame(e4, ents[3]);
		assertSame(e2, ents[4]);
	}
}
