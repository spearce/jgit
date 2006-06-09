package org.spearce.jgit.lib_tst;

import java.io.IOException;
import java.util.Iterator;

import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Tree;

public class T0002_Tree extends RepositoryTestCase
{
    private static final ObjectId SOME_FAKE_ID = new ObjectId(
        "0123456789abcdef0123456789abcdef012345678");

    public void test001_createEmpty() throws IOException
    {
        final Tree t = new Tree(r);
        assertTrue("isLoaded", t.isLoaded());
        assertTrue("isModified", t.isModified());
        assertTrue("no parent", t.getParent() == null);
        assertTrue("isRoot", t.isRoot());
        assertTrue("no name", t.getName() == null);
        assertTrue("no nameUTF8", t.getNameUTF8() == null);
        assertTrue("has iterator", t.entryIterator() != null);
        assertFalse("iterator is empty", t.entryIterator().hasNext());
        assertEquals("full name is empty", "", t.getFullName());
        assertTrue("no id", t.getId() == null);
        assertTrue("tree is self", t.getTree() == t);
        assertTrue("database is r", t.getDatabase() == r);
        assertTrue("no foo child", t.findMember("foo") == null);
    }

    public void test002_addFile() throws IOException
    {
        final Tree t = new Tree(r);
        t.setId(SOME_FAKE_ID);
        assertTrue("has id", t.getId() != null);
        assertFalse("not modified", t.isModified());

        final String n = "bob";
        final FileTreeEntry f = t.addFile(n, null);
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

        final Iterator i = t.entryIterator();
        assertTrue("iterator is not empty", i.hasNext());
        assertTrue("iterator returns file", i.next() == f);
        assertFalse("iterator is empty", i.hasNext());
    }

    public void test003_addFile() throws IOException
    {
        final Tree t = new Tree(r);

        final FileTreeEntry f = t.addFile("bob", SOME_FAKE_ID);
        assertNotNull("have file", f);
        assertTrue("id was saved", f.getId() == SOME_FAKE_ID);
    }

    public void test004_addTree() throws IOException
    {
        final Tree t = new Tree(r);
        t.setId(SOME_FAKE_ID);
        assertTrue("has id", t.getId() != null);
        assertFalse("not modified", t.isModified());

        final String n = "bob";
        final Tree f = t.addTree(n, null);
        assertNotNull("have tree", f);
        assertEquals("name matches", n, f.getName());
        assertEquals("name matches", f.getName(), new String(
            f.getNameUTF8(),
            "UTF-8"));
        assertEquals("full name matches", n, f.getFullName());
        assertTrue("no id", f.getId() == null);
        assertTrue("parent matches", f.getParent() == t);
        assertTrue("repository matches", f.getDatabase() == r);
        assertTrue("isLoaded", f.isLoaded());
        assertFalse("has items", f.entryIterator().hasNext());
        assertFalse("is root", f.isRoot());
        assertTrue("tree is self", f.getTree() == f);
        assertTrue("parent is modified", t.isModified());
        assertTrue("parent has no id", t.getId() == null);
        assertTrue("found bob child", t.findMember(f.getName()) == f);

        final Iterator i = t.entryIterator();
        assertTrue("iterator is not empty", i.hasNext());
        assertTrue("iterator returns file", i.next() == f);
        assertFalse("iterator is empty", i.hasNext());
    }

    public void test005_addTree() throws IOException
    {
        final Tree t = new Tree(r);

        final Tree f = t.addTree("bob", SOME_FAKE_ID);
        assertNotNull("have tree", f);
        assertTrue("id was saved", f.getId() == SOME_FAKE_ID);
        assertFalse("isLoaded", f.isLoaded());
    }

    public void test006_addDeepTree() throws IOException
    {
        final Tree t = new Tree(r);

        final Tree e = t.addTree("e", null);
        assertNotNull("have e", e);
        assertTrue("e.parent == t", e.getParent() == t);
        final Tree f = t.addTree("f", null);
        assertNotNull("have f", f);
        assertTrue("f.parent == t", f.getParent() == t);
        final Tree g = f.addTree("g", null);
        assertNotNull("have g", g);
        assertTrue("g.parent == f", g.getParent() == f);
        final Tree h = g.addTree("h", null);
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

        final FileTreeEntry i = h.addFile("i", null);
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
}
