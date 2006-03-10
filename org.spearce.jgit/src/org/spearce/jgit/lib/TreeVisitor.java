package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.Iterator;

public abstract class TreeVisitor {
    public void visit(final TreeEntry e) throws IOException {
        if (e instanceof Tree) {
            final Tree t = (Tree) e;
            visitTree(t);
        } else if (e instanceof FileTreeEntry) {
            visitFile((FileTreeEntry) e);
        } else if (e instanceof SymlinkTreeEntry) {
            visitSymlink((SymlinkTreeEntry) e);
        }
    }

    protected void visitTree(final Tree t) throws IOException {
        final Iterator i = t.entryIterator();
        while (i.hasNext()) {
            visit((TreeEntry) i.next());
        }
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
    }

    protected void visitSymlink(final SymlinkTreeEntry s) throws IOException {
    }
}
