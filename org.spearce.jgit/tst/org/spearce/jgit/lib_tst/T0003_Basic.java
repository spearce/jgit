/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.WriteTree;

public class T0003_Basic extends RepositoryTestCase
{
    public void test001_Initalize() throws IOException
    {
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

    public void test002_WriteEmptyTree() throws IOException
    {
        // One of our test packs contains the empty tree object. If the pack is
        // open when we create it we won't write the object file out as a loose
        // object (as it already exists in the pack).
        //
        db.closePacks();

        final Tree t = new Tree(db);
        t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
        assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
            .toString());
        final File o = new File(
            new File(new File(trash_git, "objects"), "4b"),
            "825dc642cb6eb9a060e54bf8d69288fbee4904");
        assertTrue("Exists " + o, o.isFile());
        assertTrue("Read-only " + o, !o.canWrite());
        assertEquals(15, o.length());
    }

    public void test002_WriteEmptyTree2() throws IOException
    {
        // File shouldn't exist as it is in a test pack.
        //
        final Tree t = new Tree(db);
        t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
        assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
            .toString());
        final File o = new File(
            new File(new File(trash_git, "objects"), "4b"),
            "825dc642cb6eb9a060e54bf8d69288fbee4904");
        assertFalse("Exists " + o, o.isFile());
    }

    public void test003_WriteShouldBeEmptyTree() throws IOException
    {
        final Tree t = new Tree(db);
        final ObjectId emptyId = new ObjectWriter(db).writeBlob(new byte[0]);
        t.addFile("should-be-empty").setId(emptyId);
        t.accept(new WriteTree(trash, db), TreeEntry.MODIFIED_ONLY);
        assertEquals("7bb943559a305bdd6bdee2cef6e5df2413c3d30a", t.getId()
            .toString());

        File o;
        o = new File(
            new File(new File(trash_git, "objects"), "7b"),
            "b943559a305bdd6bdee2cef6e5df2413c3d30a");
        assertTrue("Exists " + o, o.isFile());
        assertTrue("Read-only " + o, !o.canWrite());

        o = new File(
            new File(new File(trash_git, "objects"), "e6"),
            "9de29bb2d1d6434b8b29ae775ad8c2e48c5391");
        assertTrue("Exists " + o, o.isFile());
        assertTrue("Read-only " + o, !o.canWrite());
    }

    public void test004_CheckNewConfig() throws IOException
    {
        final RepositoryConfig c = db.getConfig();
        assertNotNull(c);
        assertEquals("0", c.getString("core", "repositoryformatversion"));
        assertEquals("0", c.getString("CoRe", "REPOSITORYFoRmAtVeRsIoN"));
        assertEquals("true", c.getString("core", "filemode"));
        assertEquals("true", c.getString("cOrE", "fIlEModE"));
        assertNull(c.getString("notavalue", "reallyNotAValue"));
        c.load();
    }

    public void test005_ReadSimpleConfig() throws IOException
    {
        final RepositoryConfig c = db.getConfig();
        assertNotNull(c);
        c.load();
        assertEquals("0", c.getString("core", "repositoryformatversion"));
        assertEquals("0", c.getString("CoRe", "REPOSITORYFoRmAtVeRsIoN"));
        assertEquals("true", c.getString("core", "filemode"));
        assertEquals("true", c.getString("cOrE", "fIlEModE"));
        assertNull(c.getString("notavalue", "reallyNotAValue"));
    }

    public void test006_ReadUglyConfig() throws IOException
    {
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
        assertEquals("a many line\ncomment\n to test", c.getString(
            "user",
            "defaultCheckInComment"));
        c.save();
        final FileReader fr = new FileReader(cfg);
        final char[] cbuf = new char[configStr.length()];
        fr.read(cbuf);
        fr.close();
        assertEquals(configStr, new String(cbuf));
    }

    public void test007_Open() throws IOException
    {
        final Repository db2 = new Repository(db.getDirectory());
        assertEquals(db.getDirectory(), db2.getDirectory());
        assertEquals(db.getObjectsDirectory(), db2.getObjectsDirectory());
        assertNotSame(db.getConfig(), db2.getConfig());
    }

    public void test008_FailOnWrongVersion() throws IOException
    {
        final File cfg = new File(db.getDirectory(), "config");
        final FileWriter pw = new FileWriter(cfg);
        final String badvers = "ihopethisisneveraversion";
        final String configStr = "[core]\n"
            + "\trepositoryFormatVersion="
            + badvers
            + "\n";
        pw.write(configStr);
        pw.close();

        try
        {
            new Repository(db.getDirectory());
            fail("incorrectly opened a bad repository");
        }
        catch (IOException ioe)
        {
            assertTrue(ioe.getMessage().indexOf("format") > 0);
            assertTrue(ioe.getMessage().indexOf(badvers) > 0);
        }
    }
}
