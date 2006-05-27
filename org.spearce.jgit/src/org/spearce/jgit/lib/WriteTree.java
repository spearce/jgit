package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

public class WriteTree extends TreeVisitor {
    private final Repository r;

    private final ObjectWriter ow;

    private boolean writeAll;

    private File src;

    public WriteTree(final Repository db, final File sourceDir) {
        r = db;
        ow = new ObjectWriter(r);
        writeAll = false;
        src = sourceDir;
    }

    public void setWriteAll(final boolean t) {
        writeAll = t;
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
        super.visitFile(f);
        if (f.isModified()) {
            f.setId(ow.writeBlob(new File(src, f.getName())));
        }
    }

    protected void visitSymlink(final SymlinkTreeEntry s) throws IOException {
        super.visitSymlink(s);
        if (!r.hasObject(s.getId())) {
            throw new MissingObjectException("blob", s.getId());
        }
    }

    protected void visitTree(final Tree t) throws IOException {
        // Only visit a tree if it has been modified. If any child of a tree has
        // been modified then the tree itself will also appear modified;
        // consequently we will recurse into it.
        //
        if (writeAll || t.isModified()) {
            if (t.isRoot()) {
                super.visitTree(t);
            } else {
                final File d = new File(src, t.getName());
                final File o = src;
                src = d;
                super.visitTree(t);
                src = o;
            }

            t.setId(ow.writeTree(t));
        }
    }
}
