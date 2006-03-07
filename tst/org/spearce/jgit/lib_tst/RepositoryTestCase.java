package org.spearce.jgit.lib_tst;

import java.io.File;

import junit.framework.TestCase;

import org.spearce.jgit.lib.Repository;

public abstract class RepositoryTestCase extends TestCase {
    protected static final File trash = new File("trash");

    protected static final File trash_git = new File(trash, ".git");

    protected static void recursiveDelete(final File dir) {
        final File[] ls = dir.listFiles();
        if (ls != null) {
            for (int k = 0; k < ls.length; k++) {
                final File e = ls[k];
                if (e.isDirectory()) {
                    recursiveDelete(e);
                } else {
                    e.delete();
                }
            }
        }
        dir.delete();
        if (dir.exists()) {
            throw new IllegalStateException("Failed to delete " + dir);
        }
    }

    protected Repository r;

    public void setUp() throws Exception {
        super.setUp();
        recursiveDelete(trash);
        r = new Repository(trash_git);
        r.initialize();
    }
}
