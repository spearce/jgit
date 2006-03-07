package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.PrintStream;

public class TreePrinter extends TreeVisitor {
    private final PrintStream dest;

    public TreePrinter(final PrintStream x) {
        dest = x;
    }

    protected void visitFile(final FileTreeEntry f) throws IOException {
        dest.println(f);
        super.visitFile(f);
    }

    protected void visitSymlink(final SymlinkTreeEntry s) throws IOException {
        dest.println(s);
        super.visitSymlink(s);
    }

    protected void visitTree(final Tree t) throws IOException {
        dest.println(t);
        super.visitTree(t);
    }
}
