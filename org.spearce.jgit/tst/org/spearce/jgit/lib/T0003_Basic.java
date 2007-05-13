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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class T0003_Basic extends RepositoryTestCase {
	public void test001_Initalize() {
		final File gitdir = new File(trash, ".git");
		final File objects = new File(gitdir, "objects");
		final File objects_pack = new File(objects, "pack");
		final File objects_info = new File(objects, "info");
		final File refs = new File(gitdir, "refs");
		final File refs_heads = new File(refs, "heads");
		final File refs_tags = new File(refs, "tags");
		final File HEAD = new File(gitdir, "HEAD");

		assertTrue("Exists " + trash, trash.isDirectory());
		assertTrue("Exists " + objects, objects.isDirectory());
		assertTrue("Exists " + objects_pack, objects_pack.isDirectory());
		assertTrue("Exists " + objects_info, objects_info.isDirectory());
		assertEquals(2, objects.listFiles().length);
		assertTrue("Exists " + refs, refs.isDirectory());
		assertTrue("Exists " + refs_heads, refs_heads.isDirectory());
		assertTrue("Exists " + refs_tags, refs_tags.isDirectory());
		assertTrue("Exists " + HEAD, HEAD.isFile());
		assertEquals(23, HEAD.length());
	}

	public void test002_WriteEmptyTree() throws IOException {
		// One of our test packs contains the empty tree object. If the pack is
		// open when we create it we won't write the object file out as a loose
		// object (as it already exists in the pack).
		//
		db.closePacks();

		final Tree t = new Tree(db);
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
				.toString());
		final File o = new File(new File(new File(trash_git, "objects"), "4b"),
				"825dc642cb6eb9a060e54bf8d69288fbee4904");
		assertTrue("Exists " + o, o.isFile());
		assertTrue("Read-only " + o, !o.canWrite());
		assertEquals(9, o.length());
	}

	public void test002_WriteEmptyTree2() throws IOException {
		// File shouldn't exist as it is in a test pack.
		//
		final Tree t = new Tree(db);
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
				.toString());
		final File o = new File(new File(new File(trash_git, "objects"), "4b"),
				"825dc642cb6eb9a060e54bf8d69288fbee4904");
		assertFalse("Exists " + o, o.isFile());
	}

	public void test003_WriteShouldBeEmptyTree() throws IOException {
		final Tree t = new Tree(db);
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		t.addFile("should-be-empty").setId(emptyId);
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals("7bb943559a305bdd6bdee2cef6e5df2413c3d30a", t.getId()
				.toString());

		File o;
		o = new File(new File(new File(trash_git, "objects"), "7b"),
				"b943559a305bdd6bdee2cef6e5df2413c3d30a");
		assertTrue("Exists " + o, o.isFile());
		assertTrue("Read-only " + o, !o.canWrite());

		o = new File(new File(new File(trash_git, "objects"), "e6"),
				"9de29bb2d1d6434b8b29ae775ad8c2e48c5391");
		assertTrue("Exists " + o, o.isFile());
		assertTrue("Read-only " + o, !o.canWrite());
	}

	public void test004_CheckNewConfig() throws IOException {
		final RepositoryConfig c = db.getConfig();
		assertNotNull(c);
		assertEquals("0", c.getString("core", "repositoryformatversion"));
		assertEquals("0", c.getString("CoRe", "REPOSITORYFoRmAtVeRsIoN"));
		assertEquals("true", c.getString("core", "filemode"));
		assertEquals("true", c.getString("cOrE", "fIlEModE"));
		assertNull(c.getString("notavalue", "reallyNotAValue"));
		c.load();
	}

	public void test005_ReadSimpleConfig() throws IOException {
		final RepositoryConfig c = db.getConfig();
		assertNotNull(c);
		c.load();
		assertEquals("0", c.getString("core", "repositoryformatversion"));
		assertEquals("0", c.getString("CoRe", "REPOSITORYFoRmAtVeRsIoN"));
		assertEquals("true", c.getString("core", "filemode"));
		assertEquals("true", c.getString("cOrE", "fIlEModE"));
		assertNull(c.getString("notavalue", "reallyNotAValue"));
	}

	public void test006_ReadUglyConfig() throws IOException {
		final RepositoryConfig c = db.getConfig();
		final File cfg = new File(db.getDirectory(), "config");
		final FileWriter pw = new FileWriter(cfg);
		final String configStr = "  [core];comment\n\tfilemode = yes\n"
				+ "[user]\n"
				+ "  email = A U Thor <thor@example.com> # Just an example...\n"
				+ " name = \"A  Thor \\\\ \\\"\\t \"\n"
				+ "    defaultCheckInComment = a many line\\n\\\ncomment\\n\\\n"
				+ " to test\n";
		pw.write(configStr);
		pw.close();
		c.load();
		assertEquals("yes", c.getString("core", "filemode"));
		assertEquals("A U Thor <thor@example.com>", c
				.getString("user", "email"));
		assertEquals("A  Thor \\ \"\t ", c.getString("user", "name"));
		assertEquals("a many line\ncomment\n to test", c.getString("user",
				"defaultCheckInComment"));
		c.save();
		final FileReader fr = new FileReader(cfg);
		final char[] cbuf = new char[configStr.length()];
		fr.read(cbuf);
		fr.close();
		assertEquals(configStr, new String(cbuf));
	}

	public void test007_Open() throws IOException {
		final Repository db2 = new Repository(db.getDirectory());
		assertEquals(db.getDirectory(), db2.getDirectory());
		assertEquals(db.getObjectsDirectory(), db2.getObjectsDirectory());
		assertNotSame(db.getConfig(), db2.getConfig());
	}

	public void test008_FailOnWrongVersion() throws IOException {
		final File cfg = new File(db.getDirectory(), "config");
		final FileWriter pw = new FileWriter(cfg);
		final String badvers = "ihopethisisneveraversion";
		final String configStr = "[core]\n" + "\trepositoryFormatVersion="
				+ badvers + "\n";
		pw.write(configStr);
		pw.close();

		try {
			new Repository(db.getDirectory());
			fail("incorrectly opened a bad repository");
		} catch (IOException ioe) {
			assertTrue(ioe.getMessage().indexOf("format") > 0);
			assertTrue(ioe.getMessage().indexOf(badvers) > 0);
		}
	}

	public void test009_CreateCommitOldFormat() throws IOException {
		writeTrashFile(".git/config", "[core]\n" + "legacyHeaders=1\n");
		db.getConfig().load();

		final Tree t = new Tree(db);
		final FileTreeEntry f = t.addFile("i-am-a-file");
		writeTrashFile(f.getName(), "and this is the data in me\n");
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals(new ObjectId("00b1f73724f493096d1ffa0b0f1f1482dbb8c936"),
				t.getTreeId());

		final Commit c = new Commit(db);
		c.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		c.setCommitter(new PersonIdent(jcommitter, 1154236443000L, -4 * 60));
		c.setMessage("A Commit\n");
		c.setTree(t);
		assertEquals(t.getTreeId(), c.getTreeId());
		c.commit();
		final ObjectId cmtid = new ObjectId(
				"803aec4aba175e8ab1d666873c984c0308179099");
		assertEquals(cmtid, c.getCommitId());

		// Verify the commit we just wrote is in the correct format.
		final XInputStream xis = new XInputStream(new FileInputStream(db
				.toFile(cmtid)));
		try {
			assertEquals(0x78, xis.readUInt8());
			assertEquals(0x9c, xis.readUInt8());
			assertTrue(0x789c % 31 == 0);
		} finally {
			xis.close();
		}

		// Verify we can read it.
		final Commit c2 = db.mapCommit(cmtid);
		assertNotNull(c2);
		assertEquals(c.getMessage(), c2.getMessage());
		assertEquals(c.getTreeId(), c2.getTreeId());
		assertEquals(c.getAuthor(), c2.getAuthor());
		assertEquals(c.getCommitter(), c2.getCommitter());
	}

	public void test010_CreateCommitNewFormat() throws IOException {
		writeTrashFile(".git/config", "[core]\n" + "legacyHeaders=0\n");
		db.getConfig().load();

		final Tree t = new Tree(db);
		final FileTreeEntry f = t.addFile("i-am-a-file");
		writeTrashFile(f.getName(), "and this is the data in me\n");
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals(new ObjectId("00b1f73724f493096d1ffa0b0f1f1482dbb8c936"),
				t.getTreeId());

		final Commit c = new Commit(db);
		c.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		c.setCommitter(new PersonIdent(jcommitter, 1154236443000L, -4 * 60));
		c.setMessage("A Commit\n");
		c.setTree(t);
		assertEquals(t.getTreeId(), c.getTreeId());
		c.commit();
		final ObjectId cmtid = new ObjectId(
				"803aec4aba175e8ab1d666873c984c0308179099");
		assertEquals(cmtid, c.getCommitId());

		// Verify the commit we just wrote is in the correct format.
		final XInputStream xis = new XInputStream(new FileInputStream(db
				.toFile(cmtid)));
		try {
			// 'pack style' commit header: 177 bytes
			assertEquals(0x91, xis.readUInt8());
			assertEquals(0x0b, xis.readUInt8());
			// zlib stream start
			assertEquals(0x78, xis.readUInt8());
			assertTrue(((0x78 << 8) | xis.readUInt8()) % 31 == 0);
		} finally {
			xis.close();
		}

		// Verify we can read it.
		final Commit c2 = db.mapCommit(cmtid);
		assertNotNull(c2);
		assertEquals(c.getMessage(), c2.getMessage());
		assertEquals(c.getTreeId(), c2.getTreeId());
		assertEquals(c.getAuthor(), c2.getAuthor());
		assertEquals(c.getCommitter(), c2.getCommitter());
	}

	public void test011_CreateCommitNewFormatIsDefault() throws IOException {
		db.getConfig().load();

		final Tree t = new Tree(db);
		final FileTreeEntry f = t.addFile("i-am-a-file");
		writeTrashFile(f.getName(), "and this is the data in me\n");
		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals(new ObjectId("00b1f73724f493096d1ffa0b0f1f1482dbb8c936"),
				t.getTreeId());

		final Commit c = new Commit(db);
		c.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		c.setCommitter(new PersonIdent(jcommitter, 1154236443000L, -4 * 60));
		c.setMessage("A Commit\n");
		c.setTree(t);
		assertEquals(t.getTreeId(), c.getTreeId());
		c.commit();
		final ObjectId cmtid = new ObjectId(
				"803aec4aba175e8ab1d666873c984c0308179099");
		assertEquals(cmtid, c.getCommitId());

		// Verify the commit we just wrote is in the correct format.
		final XInputStream xis = new XInputStream(new FileInputStream(db
				.toFile(cmtid)));
		try {
			// 'pack style' commit header: 177 bytes
			assertEquals(0x91, xis.readUInt8());
			assertEquals(0x0b, xis.readUInt8());
			// zlib stream start
			assertEquals(0x78, xis.readUInt8());
			assertTrue(((0x78 << 8) | xis.readUInt8()) % 31 == 0);
		} finally {
			xis.close();
		}

		// Verify we can read it.
		final Commit c2 = db.mapCommit(cmtid);
		assertNotNull(c2);
		assertEquals(c.getMessage(), c2.getMessage());
		assertEquals(c.getTreeId(), c2.getTreeId());
		assertEquals(c.getAuthor(), c2.getAuthor());
		assertEquals(c.getCommitter(), c2.getCommitter());
	}

	public void test012_SubtreeExternalSorting() throws IOException {
		final ObjectId emptyBlob = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tree t = new Tree(db);
		final FileTreeEntry e0 = t.addFile("a-");
		final FileTreeEntry e1 = t.addFile("a-b");
		final FileTreeEntry e2 = t.addFile("a/b");
		final FileTreeEntry e3 = t.addFile("a=");
		final FileTreeEntry e4 = t.addFile("a=b");

		e0.setId(emptyBlob);
		e1.setId(emptyBlob);
		e2.setId(emptyBlob);
		e3.setId(emptyBlob);
		e4.setId(emptyBlob);

		t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
		assertEquals(new ObjectId("b47a8f0a4190f7572e11212769090523e23eb1ea"),
				t.getId());
	}

	public void test020_createBlobTag() throws IOException {
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tag t = new Tag(db);
		t.setObjId(emptyId);
		t.setType("blob");
		t.setTag("test020");
		t.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		t.setMessage("test020 tagged\n");
		t.tag();
		assertEquals("6759556b09fbb4fd8ae5e315134481cc25d46954", t.getTagId().toString());

		Tag mapTag = db.mapTag("test020");
		assertEquals("blob", mapTag.getType());
		assertEquals("test020 tagged\n", mapTag.getMessage());
		assertEquals(new PersonIdent(jauthor, 1154236443000L, -4 * 60), mapTag.getAuthor());
		assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", mapTag.getObjId().toString());
	}

	public void test020b_createBlobPlainTag() throws IOException {
		test020_createBlobTag();
		Tag t = new Tag(db);
		t.setTag("test020b");
		t.setObjId(new ObjectId("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391"));
		t.tag();
		
		Tag mapTag = db.mapTag("test020b");
		assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", mapTag.getObjId().toString());
		
		// We do not repeat the plain tag test for other object types
	}

	public void test021_createTreeTag() throws IOException {
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tree almostEmptyTree = new Tree(db);
		almostEmptyTree.addEntry(new FileTreeEntry(almostEmptyTree, emptyId, "empty".getBytes(), false));
		final ObjectId almostEmptyTreeId = new ObjectWriter(db).writeTree(almostEmptyTree);
		final Tag t = new Tag(db);
		t.setObjId(almostEmptyTreeId);
		t.setType("tree");
		t.setTag("test021");
		t.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		t.setMessage("test021 tagged\n");
		t.tag();
		assertEquals("b0517bc8dbe2096b419d42424cd7030733f4abe5", t.getTagId().toString());

		Tag mapTag = db.mapTag("test021");
		assertEquals("tree", mapTag.getType());
		assertEquals("test021 tagged\n", mapTag.getMessage());
		assertEquals(new PersonIdent(jauthor, 1154236443000L, -4 * 60), mapTag.getAuthor());
		assertEquals("417c01c8795a35b8e835113a85a5c0c1c77f67fb", mapTag.getObjId().toString());
	}

	public void test022_createCommitTag() throws IOException {
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tree almostEmptyTree = new Tree(db);
		almostEmptyTree.addEntry(new FileTreeEntry(almostEmptyTree, emptyId, "empty".getBytes(), false));
		final ObjectId almostEmptyTreeId = new ObjectWriter(db).writeTree(almostEmptyTree);
		final Commit almostEmptyCommit = new Commit(db);
		almostEmptyCommit.setAuthor(new PersonIdent(jauthor, 1154236443000L, -2 * 60)); // not exactly the same
		almostEmptyCommit.setCommitter(new PersonIdent(jauthor, 1154236443000L, -2 * 60));
		almostEmptyCommit.setMessage("test022\n");
		almostEmptyCommit.setTreeId(almostEmptyTreeId);
		ObjectId almostEmptyCommitId = new ObjectWriter(db).writeCommit(almostEmptyCommit);
		final Tag t = new Tag(db);
		t.setObjId(almostEmptyCommitId);
		t.setType("commit");
		t.setTag("test022");
		t.setAuthor(new PersonIdent(jauthor, 1154236443000L, -4 * 60));
		t.setMessage("test022 tagged\n");
		t.tag();
		assertEquals("0ce2ebdb36076ef0b38adbe077a07d43b43e3807", t.getTagId().toString());

		Tag mapTag = db.mapTag("test022");
		assertEquals("commit", mapTag.getType());
		assertEquals("test022 tagged\n", mapTag.getMessage());
		assertEquals(new PersonIdent(jauthor, 1154236443000L, -4 * 60), mapTag.getAuthor());
		assertEquals("b5d3b45a96b340441f5abb9080411705c51cc86c", mapTag.getObjId().toString());
	}
	
	public void test023_createCommitNonAscii() throws IOException {
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tree almostEmptyTree = new Tree(db);
		almostEmptyTree.addEntry(new FileTreeEntry(almostEmptyTree, emptyId, "empty".getBytes(), false));
		final ObjectId almostEmptyTreeId = new ObjectWriter(db).writeTree(almostEmptyTree);
		Commit commit = new Commit(db);
		commit.setTreeId(almostEmptyTreeId);
		commit.setAuthor(new PersonIdent("Joe H\u00e4cker","joe@example.com",4294967295000L,60));
		commit.setCommitter(new PersonIdent("Joe Hacker","joe2@example.com",4294967295000L,60));
		commit.setEncoding("UTF-8");
		commit.setMessage("\u00dcbergeeks");
		ObjectId cid = new ObjectWriter(db).writeCommit(commit);
		assertEquals("4680908112778718f37e686cbebcc912730b3154", cid.toString());
	}

	public void test024_createCommitNonAscii() throws IOException {
		final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
		final Tree almostEmptyTree = new Tree(db);
		almostEmptyTree.addEntry(new FileTreeEntry(almostEmptyTree, emptyId, "empty".getBytes(), false));
		final ObjectId almostEmptyTreeId = new ObjectWriter(db).writeTree(almostEmptyTree);
		Commit commit = new Commit(db);
		commit.setTreeId(almostEmptyTreeId);
		commit.setAuthor(new PersonIdent("Joe H\u00e4cker","joe@example.com",4294967295000L,60));
		commit.setCommitter(new PersonIdent("Joe Hacker","joe2@example.com",4294967295000L,60));
		commit.setEncoding("ISO-8859-1");
		commit.setMessage("\u00dcbergeeks");
		ObjectId cid = new ObjectWriter(db).writeCommit(commit);
		assertEquals("2979b39d385014b33287054b87f77bcb3ecb5ebf", cid.toString());
	}
}
