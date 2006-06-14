package org.spearce.jgit.lib_tst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

public class T0002_Tree extends RepositoryTestCase
{
    private static final ObjectId SOME_FAKE_ID = new ObjectId(
        "0123456789abcdef0123456789abcdef01234567");

    public void test001_createEmpty() throws IOException
    {
        final Tree t = new Tree(db);
        assertTrue("isLoaded", t.isLoaded());
        assertTrue("isModified", t.isModified());
        assertTrue("no parent", t.getParent() == null);
        assertTrue("isRoot", t.isRoot());
        assertTrue("no name", t.getName() == null);
        assertTrue("no nameUTF8", t.getNameUTF8() == null);
        assertTrue("has entries array", t.entries() != null);
        assertTrue("entries is empty", t.entries().length == 0);
        assertEquals("full name is empty", "", t.getFullName());
        assertTrue("no id", t.getId() == null);
        assertTrue("tree is self", t.getTree() == t);
        assertTrue("database is r", t.getRepository() == db);
        assertTrue("no foo child", t.findMember("foo") == null);
    }

    public void test002_addFile() throws IOException
    {
        final Tree t = new Tree(db);
        t.setId(SOME_FAKE_ID);
        assertTrue("has id", t.getId() != null);
        assertFalse("not modified", t.isModified());

        final String n = "bob";
        final FileTreeEntry f = t.addFile(n);
        assertNotNull("have file", f);
        assertEquals("name matches", n, f.getName());
        assertEquals("name matches", f.getName(), new String(
            f.getNameUTF8(),
            "UTF-8"));
        assertEquals("full name matches", n, f.getFullName());
        assertTrue("no id", f.getId() == null);
        assertTrue("is modified", t.isModified());
        assertTrue("has no id", t.getId() == null);
        assertTrue("found bob", t.findMember(f.getName()) == f);

        final TreeEntry[] i = t.entries();
        assertTrue("iterator is not empty", i != null && i.length > 0);
        assertTrue("iterator returns file", i[0] == f);
        assertTrue("iterator is empty", i.length == 1);
    }

    public void test004_addTree() throws IOException
    {
        final Tree t = new Tree(db);
        t.setId(SOME_FAKE_ID);
        assertTrue("has id", t.getId() != null);
        assertFalse("not modified", t.isModified());

        final String n = "bob";
        final Tree f = t.addTree(n);
        assertNotNull("have tree", f);
        assertEquals("name matches", n, f.getName());
        assertEquals("name matches", f.getName(), new String(
            f.getNameUTF8(),
            "UTF-8"));
        assertEquals("full name matches", n, f.getFullName());
        assertTrue("no id", f.getId() == null);
        assertTrue("parent matches", f.getParent() == t);
        assertTrue("repository matches", f.getRepository() == db);
        assertTrue("isLoaded", f.isLoaded());
        assertFalse("has items", f.entries().length > 0);
        assertFalse("is root", f.isRoot());
        assertTrue("tree is self", f.getTree() == f);
        assertTrue("parent is modified", t.isModified());
        assertTrue("parent has no id", t.getId() == null);
        assertTrue("found bob child", t.findMember(f.getName()) == f);

        final TreeEntry[] i = t.entries();
        assertTrue("iterator is not empty", i.length > 0);
        assertTrue("iterator returns file", i[0] == f);
        assertTrue("iterator is empty", i.length == 1);
    }

    public void test005_linkTree() throws IOException
    {
        final Tree t = new Tree(db);

        final Tree f = t.linkTree("bob", SOME_FAKE_ID);
        assertNotNull("have tree", f);
        assertTrue("id was saved", f.getId() == SOME_FAKE_ID);
        assertFalse("isLoaded", f.isLoaded());
    }

    public void test006_addDeepTree() throws IOException
    {
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
        assertTrue("Can find h", t.findMember(h.getFullName()) == h);
        assertTrue("Can't find f/z", t.findMember("f/z") == null);
        assertTrue("Can't find y/z", t.findMember("y/z") == null);

        final FileTreeEntry i = h.addFile("i");
        assertNotNull(i);
        assertEquals("full path of i ok", "f/g/h/i", i.getFullName());
        assertTrue("Can find i", t.findMember(i.getFullName()) == i);
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

    public void test007_manyFileLookup() throws IOException
    {
        final Tree t = new Tree(db);
        final List files = new ArrayList(26 * 26);
        for (char level1 = 'a'; level1 <= 'z'; level1++)
        {
            for (char level2 = 'a'; level2 <= 'z'; level2++)
            {
                final String n = "." + level1 + level2 + "9";
                final FileTreeEntry f = t.addFile(n);
                assertNotNull("File " + n + " added.", f);
                assertEquals(n, f.getName());
                files.add(f);
            }
        }
        assertEquals(files.size(), t.entryCount());
        final TreeEntry[] ents = t.entries();
        assertNotNull(ents);
        assertEquals(files.size(), ents.length);
        for (int k = 0; k < ents.length; k++)
        {
            assertTrue("File "
                + ((FileTreeEntry) files.get(k)).getName()
                + " is at "
                + k
                + ".", files.get(k) == ents[k]);
        }
    }
}
