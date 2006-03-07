package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.CopyTreeToDirectory;
import org.spearce.jgit.lib.ObjectDatabase;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreePrinter;
import org.spearce.jgit.lib.WriteTree;

public class ObjectDatabaseTest extends TestCase {
    public void testConnect() throws IOException {
        final ObjectDatabase d;
        final Commit c;

        d = new ObjectDatabase(new File(
                "/Users/spearce/cgwork/PlayAreas/test1/.git"));
        final String id = "25a4dc8a35d3c288e5eaa366ed95bb0c08507ec7";
        c = d.mapCommit(new ObjectId(id));
        assertNotNull(c);
        assertEquals(id, c.getCommitId().toString());
        assertNotNull(c.getTreeId());
        assertNotNull(c.getParentIds());
        assertTrue(c.getParentIds().size() > 0);
        assertNotNull(c.getAuthor());
        assertNotNull(c.getCommitter());

        System.out.println("commit=" + c.getCommitId());
        System.out.println("tree=" + c.getTreeId());
        System.out.println("parents=" + c.getParentIds());
        System.out.println("author=" + c.getAuthor());
        System.out.println("committer=" + c.getCommitter());
        System.out.println("message={" + c.getMessage() + "}");

        final Tree t = d.mapTree(c.getTreeId());
        assertNotNull(t);
        assertEquals(c.getTreeId(), t.getTreeId());
        new TreePrinter(System.err).visit(t);
    }

    public void testReadHEAD() throws IOException {
        final ObjectDatabase d;
        final Commit c;

        d = new ObjectDatabase(new File(
                "/Users/spearce/cgwork/PlayAreas/test1/.git"));
        final ObjectId id = d.resolveRevision("HEAD");
        c = d.mapCommit(id);
        assertNotNull(c);
        assertEquals(id, c.getCommitId());
        assertNotNull(c.getTreeId());
        assertNotNull(c.getParentIds());
        assertTrue(c.getParentIds().size() > 0);
        assertNotNull(c.getAuthor());
        assertNotNull(c.getCommitter());

        System.out.println("commit=" + c.getCommitId());
        System.out.println("tree=" + c.getTreeId());
        System.out.println("parents=" + c.getParentIds());
        System.out.println("author=" + c.getAuthor());
        System.out.println("committer=" + c.getCommitter());
        System.out.println("message={" + c.getMessage() + "}");
    }

    public void testCopyOut() throws IOException {
        final ObjectDatabase d;

        d = new ObjectDatabase(new File(
                "/Users/spearce/cgwork/PlayAreas/test1/.git"));
        final String id = "facb516c64c3ab5729a457b2f2aa42f7d6feafd1";
        final Tree t = d.mapTree(new ObjectId(id));
        new CopyTreeToDirectory(new File(
                "/Users/spearce/cgwork/PlayAreas/Eclipse2")).visit(t);
    }

    public void testWriteTree() throws NoSuchAlgorithmException, IOException {
        final ObjectDatabase d1;
        final ObjectDatabase d2;

        d1 = new ObjectDatabase(new File(
                "/Users/spearce/cgwork/PlayAreas/test1/.git"));
        d2 = new ObjectDatabase(new File(
                "/Users/spearce/cgwork/PlayAreas/test4/.git"));
        final String id = "facb516c64c3ab5729a457b2f2aa42f7d6feafd1";
        final Tree t = d1.mapTree(new ObjectId(id));
        new WriteTree(d2, new File("/Users/spearce/cgwork/PlayAreas/Eclipse2"))
                .visit(t);
    }
}
