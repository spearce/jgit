package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WriteTree;

public class T0003_Basic extends RepositoryTestCase {
    public void test001_Initalize() throws IOException {
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
        r.closePacks();

        final Tree t = new Tree(r);
        new WriteTree(r, trash).visit(t);
        assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
                .toString());
        final File o = new File(new File(new File(trash_git, "objects"), "4b"),
                "825dc642cb6eb9a060e54bf8d69288fbee4904");
        assertTrue("Exists " + o, o.isFile());
        assertTrue("Read-only " + o, !o.canWrite());
        assertEquals(15, o.length());
    }

    public void test002_WriteEmptyTree2() throws IOException {
        // File shouldn't exist as it is in a test pack.
        //
        final Tree t = new Tree(r);
        new WriteTree(r, trash).visit(t);
        assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", t.getId()
                .toString());
        final File o = new File(new File(new File(trash_git, "objects"), "4b"),
                "825dc642cb6eb9a060e54bf8d69288fbee4904");
        assertFalse("Exists " + o, o.isFile());
    }

    public void test003_WriteShouldBeEmptyTree() throws IOException {
        final Tree t = new Tree(r);
        final ObjectId emptyId = new ObjectWriter(r).writeBlob(new byte[0]);
        t.addFile("should-be-empty", emptyId);
        new WriteTree(r, trash).visit(t);
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
}
