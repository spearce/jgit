package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.spearce.jgit.lib.FullRepository;

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

    protected static void copyFile(final File src, final File dst)
            throws IOException {
        final FileInputStream fis = new FileInputStream(src);
        final FileOutputStream fos = new FileOutputStream(dst);
        final byte[] buf = new byte[4096];
        int r;
        while ((r = fis.read(buf)) > 0) {
            fos.write(buf, 0, r);
        }
        fis.close();
        fos.close();
    }

    protected FullRepository r;

    public void setUp() throws Exception {
        super.setUp();
        recursiveDelete(trash);
        r = new FullRepository(trash_git);
        r.create();

        final String[] packs = { "pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f" };
        final File tst = new File("tst");
        final File packDir = new File(r.getObjectsDirectory(), "pack");
        for (int k = 0; k < packs.length; k++) {
            copyFile(new File(tst, packs[k] + ".pack"), new File(packDir,
                    packs[k] + ".pack"));
            copyFile(new File(tst, packs[k] + ".idx"), new File(packDir,
                    packs[k] + ".idx"));
        }

        r.scanForPacks();
    }

    protected void tearDown() throws Exception {
        r.close();
        super.tearDown();
    }
}
